package com.yachay.app.data.db

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Migrações rodadas num SQLite da JVM: o banco migrado precisa ter as mesmas tabelas, colunas,
 * chaves e índices que o Room espera (schemas exportados em `app/schemas`) e manter os dados.
 */
class MigrationsTest {

    private fun schema(version: Int): JsonObject {
        // Os testes rodam com o diretório do módulo como raiz.
        val file = File("schemas/com.yachay.app.data.db.YachayDatabase/$version.json")
        return Json.parseToJsonElement(file.readText()).jsonObject["database"]!!.jsonObject
    }

    /** CREATE TABLE/INDEX de cada entidade do schema exportado. */
    private fun createStatements(version: Int): List<String> =
        schema(version)["entities"]!!.jsonArray.flatMap { element ->
            val entity = element.jsonObject
            val table = entity["tableName"]!!.jsonPrimitive.content
            val indices = entity["indices"]?.jsonArray.orEmpty().map { it.jsonObject["createSql"]!!.jsonPrimitive.content }
            (listOf(entity["createSql"]!!.jsonPrimitive.content) + indices).map { it.replace("\${TABLE_NAME}", table) }
        }

    private fun open(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    private fun Connection.exec(sql: String) = createStatement().use { it.execute(sql) }

    private fun Connection.rows(sql: String): List<List<String?>> = createStatement().use { st ->
        st.executeQuery(sql).use { rs ->
            val columns = rs.metaData.columnCount
            buildList { while (rs.next()) add((1..columns).map { rs.getString(it) }) }
        }
    }

    /** O que o Room compara ao validar: colunas, chaves estrangeiras e índices. */
    private fun Connection.describe(table: String): List<List<List<String?>>> = listOf(
        rows("PRAGMA table_info(`$table`)"),
        rows("PRAGMA foreign_key_list(`$table`)").map { it.drop(2) }, // id/seq variam
        rows("PRAGMA index_list(`$table`)").map { it.drop(1) }.sortedBy { it.first() },
    )

    private fun tables(version: Int) =
        schema(version)["entities"]!!.jsonArray.map { it.jsonObject["tableName"]!!.jsonPrimitive.content }

    @Test
    fun `migration 1 to 2 matches the exported schema and keeps the library`() {
        val migrated = open()
        createStatements(1).forEach { migrated.exec(it) }
        migrated.exec("INSERT INTO books (id, uri, title, format, progress, addedAt) VALUES (1, 'content://a', 'Dom Casmurro', 'EPUB', 0.5, 0)")
        migrated.exec("INSERT INTO collections (id, name, createdAt) VALUES (1, 'Clássicos', 0)")
        migrated.exec("INSERT INTO book_collection (bookId, collectionId) VALUES (1, 1)")

        Migrations.SQL_1_2.forEach { migrated.exec(it) }

        val fresh = open()
        createStatements(2).forEach { fresh.exec(it) }
        tables(2).forEach { assertEquals(it, fresh.describe(it), migrated.describe(it)) }

        assertEquals(listOf(listOf("Dom Casmurro", "0.5")), migrated.rows("SELECT title, progress FROM books"))
        assertEquals(listOf(listOf("1")), migrated.rows("SELECT COUNT(*) FROM book_collection"))
    }

    @Test
    fun `removing a book deletes its annotations`() {
        val db = open()
        createStatements(1).forEach { db.exec(it) }
        Migrations.SQL_1_2.forEach { db.exec(it) }
        db.exec("PRAGMA foreign_keys = ON") // o Room liga isso em toda conexão
        db.exec("INSERT INTO books (id, uri, title, format, progress, addedAt) VALUES (1, 'content://a', 'A', 'PDF', 0, 0)")
        db.exec("INSERT INTO books (id, uri, title, format, progress, addedAt) VALUES (2, 'content://b', 'B', 'PDF', 0, 0)")
        db.exec("INSERT INTO annotations (bookId, type, position, createdAt) VALUES (1, 'BOOKMARK', '11', 0)")
        db.exec("INSERT INTO annotations (bookId, type, position, createdAt) VALUES (2, 'BOOKMARK', '3', 0)")

        db.exec("DELETE FROM books WHERE id = 1")

        assertEquals(listOf(listOf("2")), db.rows("SELECT bookId FROM annotations"))
    }

    @Test
    fun `reading day upsert keeps one row per date`() {
        val db = open()
        createStatements(2).forEach { db.exec(it) }
        // Mesma semântica do @Upsert: o dia guarda o valor absoluto mais recente.
        db.exec("INSERT INTO reading_days (date, pages, goalMet) VALUES ('2026-11-02', 4, 0) ON CONFLICT(date) DO UPDATE SET pages = excluded.pages, goalMet = excluded.goalMet")
        db.exec("INSERT INTO reading_days (date, pages, goalMet) VALUES ('2026-11-02', 10, 1) ON CONFLICT(date) DO UPDATE SET pages = excluded.pages, goalMet = excluded.goalMet")
        db.exec("INSERT INTO reading_days (date, pages, goalMet) VALUES ('2026-10-30', 2, 0)")
        assertEquals(
            listOf(listOf("2026-11-02", "10", "1")),
            db.rows("SELECT date, pages, goalMet FROM reading_days WHERE date BETWEEN '2026-11-01' AND '2026-11-30'"),
        )
    }
}
