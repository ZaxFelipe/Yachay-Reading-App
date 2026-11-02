package com.yachay.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class, CollectionEntity::class, BookCollectionCrossRef::class],
    version = 1,
    exportSchema = false,
)
abstract class YachayDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        fun create(context: Context): YachayDatabase =
            Room.databaseBuilder(context, YachayDatabase::class.java, "yachay.db").build()
    }
}
