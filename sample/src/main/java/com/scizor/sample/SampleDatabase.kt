package com.scizor.sample

import android.content.Context
import android.database.DatabaseUtils
import kotlin.random.Random

/**
 * A small SQLite database of demo records (users, posts, products) so Scizor's
 * Database Browser has realistic data to inspect — mirroring the SwiftData models
 * in Scyther's example app.
 */
object SampleDatabase {

    private const val DB = "demo.db"

    data class Counts(val users: Int, val posts: Int, val products: Int)

    fun seed(context: Context) {
        db(context) { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS users " +
                    "(id INTEGER PRIMARY KEY, name TEXT, email TEXT, age INTEGER, createdAt INTEGER)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS posts " +
                    "(id INTEGER PRIMARY KEY, title TEXT, content TEXT, publishedAt INTEGER, userId INTEGER)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS products " +
                    "(id INTEGER PRIMARY KEY, name TEXT, price REAL, inStock INTEGER, category TEXT)",
            )
            if (DatabaseUtils.queryNumEntries(db, "users") > 0L) return@db

            val now = System.currentTimeMillis()
            listOf(
                Triple("Alice Johnson", "alice@example.com", 28),
                Triple("Bob Smith", "bob@example.com", 34),
                Triple("Charlie Brown", "charlie@example.com", 22),
                Triple("Diana Ross", "diana@example.com", 45),
                Triple("Eve Williams", "eve@example.com", 31),
            ).forEach { (name, email, age) ->
                db.execSQL(
                    "INSERT INTO users (name, email, age, createdAt) VALUES (?, ?, ?, ?)",
                    arrayOf(name, email, age, now),
                )
            }
            listOf(
                "Getting Started with Room" to 1,
                "Android Development Tips" to 1,
                "Building Debug Tools" to 2,
                "Compose Best Practices" to 3,
                "Understanding Migrations" to 4,
                "Unit Testing in Kotlin" to 5,
            ).forEach { (title, userId) ->
                db.execSQL(
                    "INSERT INTO posts (title, content, publishedAt, userId) VALUES (?, ?, ?, ?)",
                    arrayOf(title, "Sample content for “$title”.", now, userId),
                )
            }
            listOf(
                arrayOf<Any>("Pixel 9 Pro", 999.0, 1, "Electronics"),
                arrayOf<Any>("Pixelbook", 1299.0, 1, "Electronics"),
                arrayOf<Any>("Pixel Buds Pro", 199.0, 0, "Audio"),
                arrayOf<Any>("Mechanical Keyboard", 149.0, 1, "Accessories"),
                arrayOf<Any>("Pixel Watch 3", 349.0, 1, "Wearables"),
                arrayOf<Any>("Pixel Tablet", 499.0, 1, "Tablets"),
                arrayOf<Any>("Studio Monitor", 599.0, 0, "Displays"),
                arrayOf<Any>("Nest Mini", 49.0, 1, "Audio"),
            ).forEach { row ->
                db.execSQL("INSERT INTO products (name, price, inStock, category) VALUES (?, ?, ?, ?)", row)
            }
        }
    }

    fun counts(context: Context): Counts = db(context) { db ->
        Counts(
            users = DatabaseUtils.queryNumEntries(db, "users").toInt(),
            posts = DatabaseUtils.queryNumEntries(db, "posts").toInt(),
            products = DatabaseUtils.queryNumEntries(db, "products").toInt(),
        )
    } ?: Counts(0, 0, 0)

    fun addRandomRecords(context: Context) {
        db(context) { db ->
            val n = Random.nextInt(1000, 9999)
            db.execSQL(
                "INSERT INTO users (name, email, age, createdAt) VALUES (?, ?, ?, ?)",
                arrayOf("User $n", "user$n@example.com", Random.nextInt(18, 65), System.currentTimeMillis()),
            )
            db.execSQL(
                "INSERT INTO posts (title, content, publishedAt, userId) VALUES (?, ?, ?, ?)",
                arrayOf("Post $n", "Randomly generated post.", System.currentTimeMillis(), 1),
            )
            val categories = listOf("Electronics", "Audio", "Accessories", "Wearables", "Software")
            db.execSQL(
                "INSERT INTO products (name, price, inStock, category) VALUES (?, ?, ?, ?)",
                arrayOf(
                    "Product $n",
                    Random.nextInt(10, 999).toDouble(),
                    if (Random.nextBoolean()) 1 else 0,
                    categories.random(),
                ),
            )
        }
    }

    fun clear(context: Context) {
        db(context) { db ->
            db.execSQL("DELETE FROM users")
            db.execSQL("DELETE FROM posts")
            db.execSQL("DELETE FROM products")
        }
    }

    private fun <T> db(context: Context, block: (android.database.sqlite.SQLiteDatabase) -> T): T? =
        runCatching {
            context.openOrCreateDatabase(DB, Context.MODE_PRIVATE, null).use(block)
        }.getOrNull()
}
