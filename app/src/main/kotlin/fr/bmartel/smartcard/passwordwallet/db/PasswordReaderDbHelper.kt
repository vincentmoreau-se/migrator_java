/*********************************************************************************
 * This file is part of SIM Password Wallet                                      *
 * <p/>                                                                          *
 * Copyright (C) 2017  Bertrand Martel                                           *
 * <p/>                                                                          *
 * SIM Password Wallet is free software: you can redistribute it and/or modify   *
 * it under the terms of the GNU General Public License as published by          *
 * the Free Software Foundation, either version 3 of the License, or             *
 * (at your option) any later version.                                           *
 * <p/>                                                                          *
 * SIM Password Wallet is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                 *
 * GNU General Public License for more details.                                  *
 * <p/>                                                                          *
 * You should have received a copy of the GNU General Public License             *
 * along with SIM Password Wallet.  If not, see <http://www.gnu.org/licenses/>.  *
 */
package fr.bmartel.smartcard.passwordwallet.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

/**
 * Helper for local database.
 *
 * @author Bertrand Martel
 */
class PasswordReaderDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    object PasswordEntry : BaseColumns {
        const val TABLE_NAME = "pass"
        const val COLUMN_NAME_TITLE = "title"
        const val COLUMN_NAME_USERNAME = "username"
        const val COLUMN_NAME_PASSWORD = "password"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        const val TITLE_MAX_SIZE: Short = 32
        const val USERNAME_MAX_SIZE: Short = 64
        const val PASSWORD_MAX_SIZE: Short = 127

        /**
         * create table.
         */
        private val SQL_CREATE_ENTRIES =
            "CREATE TABLE ${PasswordEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${PasswordEntry.COLUMN_NAME_TITLE} VARCHAR($TITLE_MAX_SIZE)," +
                    "${PasswordEntry.COLUMN_NAME_USERNAME} VARCHAR($USERNAME_MAX_SIZE)," +
                    "${PasswordEntry.COLUMN_NAME_PASSWORD} VARCHAR($PASSWORD_MAX_SIZE))"

        /**
         * drop table.
         */
        private val SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS ${PasswordEntry.TABLE_NAME}"

        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "PasswordReader.db"
    }
}
