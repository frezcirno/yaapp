package com.frezcirno.weather.preferences

import android.database.sqlite.SQLiteOpenHelper
import com.frezcirno.weather.preferences.DBHelper
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.content.Context
import java.util.ArrayList

class DBHelper(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_QUESTION_TABLE =
            "CREATE TABLE $TABLE_CITIES ($KEY_CITY TEXT,$KEY_ID INTEGER PRIMARY KEY);"
        db.execSQL(CREATE_QUESTION_TABLE)
    }

    // Upgrading database  
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if existed  
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CITIES")

        // Create tables again  
        onCreate(db)
    }

    fun addCity(string: String?) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_CITY, string)
        db.insert(TABLE_CITIES, null, values)
        db.close()
    }

    val cities: List<String>
        get() {
            val db = this.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_CITIES ORDER BY $KEY_ID DESC;",
                null
            )
            val categoryList: MutableList<String> = ArrayList()
            if (cursor != null) {
                cursor.moveToFirst()
                if (cursor.moveToFirst()) {
                    do {
                        categoryList.add(cursor.getString(0))
                    } while (cursor.moveToNext())
                }
                cursor.close()
            }
            return categoryList
        }

    fun cityExists(string: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $KEY_CITY FROM $TABLE_CITIES;", null)
            ?: return false
        while (cursor.moveToNext()) {
            if (cursor.getString(0) == string) {
                cursor.close()
                return true
            }
        }
        cursor.close()
        return false
    }

    fun deleteCity(string: String) {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM $TABLE_CITIES WHERE $KEY_CITY LIKE '$string'")
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "WeatherDatabase.db"
        private const val TABLE_CITIES = "cities"
        private const val KEY_CITY = "city"
        private const val KEY_ID = "id"
    }
}