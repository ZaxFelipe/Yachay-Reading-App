package com.yachay.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        CollectionEntity::class,
        BookCollectionCrossRef::class,
        AnnotationEntity::class,
        ReadingDayEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class YachayDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun collectionDao(): CollectionDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun readingDayDao(): ReadingDayDao

    companion object {
        fun create(context: Context): YachayDatabase =
            Room.databaseBuilder(context, YachayDatabase::class.java, "yachay.db")
                .addMigrations(*Migrations.ALL)
                .build()
    }
}
