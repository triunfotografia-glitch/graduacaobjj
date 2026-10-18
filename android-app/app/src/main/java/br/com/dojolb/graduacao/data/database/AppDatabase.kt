package br.com.dojolb.graduacao.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.dojolb.graduacao.data.dao.PositionDao
import br.com.dojolb.graduacao.data.dao.StudentDao
import br.com.dojolb.graduacao.data.entity.Position
import br.com.dojolb.graduacao.data.entity.Student

@Database(
    entities = [Student::class, Position::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun positionDao(): PositionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dojo_graduacao.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    // Add type converters if needed for complex types
}