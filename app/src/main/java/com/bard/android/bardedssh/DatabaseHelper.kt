package com.bard.android.bardedssh

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

val DB_DEBUG = false
class MachineSettingsDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "MachineSettings.db"
        private const val TABLE_NAME = "machine_settings"
        private const val COLUMN_ID = "_id"
        private const val COLUMN_HOSTNAME = "hostname"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_PORT = "port"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_HOSTNAME TEXT," +
                "$COLUMN_USERNAME TEXT," +
                "$COLUMN_PASSWORD TEXT," +
                "$COLUMN_PORT INTEGER" +
                ")"
        //val dropTable = "DROP TABLE $TABLE_NAME"
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    fun deleteSettings(settings: MachineSettings){
        val db = writableDatabase
        db.delete(
        TABLE_NAME, "$COLUMN_HOSTNAME = ? AND $COLUMN_USERNAME = ? AND $COLUMN_PORT = ?",
        arrayOf(settings.hostname, settings.username, settings.port.toString()))
    }
    fun insertSettings(settings: MachineSettings) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_HOSTNAME, settings.hostname)
            put(COLUMN_USERNAME, settings.username)
            put(COLUMN_PASSWORD, settings.password)
            put(COLUMN_PORT, settings.port)
        }
        db.insert(TABLE_NAME, null, values)
    }

    @SuppressLint("Range")
    fun getAllSettings(): List<MachineSettings> {
        val settingsList = mutableListOf<MachineSettings>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndex(COLUMN_ID))
                val hostname = getString(getColumnIndex(COLUMN_HOSTNAME))
                val username = getString(getColumnIndex(COLUMN_USERNAME))
                val password = getString(getColumnIndex(COLUMN_PASSWORD))
                val port = getInt(getColumnIndex(COLUMN_PORT))
                val settings = MachineSettings(hostname, username, password, port)
                settingsList.add(settings)
            }
        }
        return settingsList
    }
}