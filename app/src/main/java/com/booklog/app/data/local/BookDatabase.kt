package com.booklog.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Book::class,
        KidProfile::class,
        ReadingDayLog::class,
        RewardTransaction::class,
        CompletedBook::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(ReadingStatusConverter::class)
abstract class BookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun kidProfileDao(): KidProfileDao
    abstract fun readingDayLogDao(): ReadingDayLogDao
    abstract fun rewardTransactionDao(): RewardTransactionDao
    abstract fun completedBookDao(): CompletedBookDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN publisher TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN genre TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN kidProfileId INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS kid_profiles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        emoji TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reading_day_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        kidProfileId INTEGER,
                        bookId INTEGER,
                        dayKey TEXT NOT NULL,
                        pagesLogged INTEGER NOT NULL,
                        loggedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reward_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        kidProfileId INTEGER,
                        amountCents INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE kid_profiles ADD COLUMN gender TEXT NOT NULL DEFAULT 'PREFER_NOT_TO_SAY'")
                db.execSQL("ALTER TABLE kid_profiles ADD COLUMN dateOfBirth INTEGER")
                db.execSQL("ALTER TABLE kid_profiles ADD COLUMN favoriteGenre TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE kid_profiles ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS completed_books (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        kidProfileId INTEGER,
                        bookId INTEGER NOT NULL,
                        isbn TEXT,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        pageCount INTEGER,
                        minutesRead INTEGER NOT NULL,
                        dateCompleted INTEGER NOT NULL,
                        readCountNumber INTEGER NOT NULL,
                        isReread INTEGER NOT NULL,
                        pageRewardCents INTEGER NOT NULL,
                        timeRewardCents INTEGER NOT NULL,
                        totalRewardCents INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "ALTER TABLE reward_transactions ADD COLUMN direction TEXT NOT NULL DEFAULT 'DEBIT'",
                )
                db.execSQL(
                    "ALTER TABLE reward_transactions ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'DEBIT_REDEEMED'",
                )
                db.execSQL(
                    "ALTER TABLE reward_transactions ADD COLUMN pageRewardCents INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE reward_transactions ADD COLUMN timeRewardCents INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE reward_transactions ADD COLUMN bonusRewardCents INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE reward_transactions ADD COLUMN bookId INTEGER")
                db.execSQL("ALTER TABLE reward_transactions ADD COLUMN completedBookId INTEGER")
                db.execSQL(
                    "ALTER TABLE reward_transactions ADD COLUMN balanceBefore INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE reward_transactions ADD COLUMN balanceAfter INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        @Volatile
        private var instance: BookDatabase? = null

        fun getInstance(context: Context): BookDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookDatabase::class.java,
                    "booklog.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}