package com.yachay.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrações do `yachay.db`. Nunca usar `fallbackToDestructiveMigration`: apagaria a biblioteca.
 * A SQL fica em listas puras para o teste rodar num SQLite da JVM e comparar com `app/schemas`.
 */
object Migrations {

    /** 0.4.0: marcadores/destaques e histórico de dias lidos. */
    val SQL_1_2 = listOf(
        "CREATE TABLE IF NOT EXISTS `annotations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bookId` INTEGER NOT NULL, `type` TEXT NOT NULL, `position` TEXT NOT NULL, `text` TEXT, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_annotations_bookId` ON `annotations` (`bookId`)",
        "CREATE TABLE IF NOT EXISTS `reading_days` (`date` TEXT NOT NULL, `pages` INTEGER NOT NULL, `goalMet` INTEGER NOT NULL, PRIMARY KEY(`date`))",
    )

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            SQL_1_2.forEach(db::execSQL)
        }
    }

    val ALL = arrayOf(MIGRATION_1_2)
}
