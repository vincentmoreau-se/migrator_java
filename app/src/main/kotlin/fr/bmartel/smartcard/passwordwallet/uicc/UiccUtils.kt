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
package fr.bmartel.smartcard.passwordwallet.uicc

import fr.bmartel.smartcard.passwordwallet.model.Password

/**
 * UICC utility functions used to generate/parse payload.
 *
 * @author Bertrand Martel
 */
object UiccUtils {

    /**
     * Build create password entry payload.
     *
     * @param title password title
     * @param username username
     * @param password password value
     * @return data payload
     */
    @JvmStatic
    fun buildAddPassword(title: String, username: String, password: String): ByteArray {
        val titleBa = title.toByteArray()
        val usernameBa = username.toByteArray()
        val passwordBa = password.toByteArray()

        val res = ByteArray(titleBa.size + usernameBa.size + passwordBa.size + 3 + 3)

        var offset = 0

        res[offset++] = 0xF1.toByte()
        res[offset++] = titleBa.size.toByte()
        System.arraycopy(titleBa, 0, res, offset, titleBa.size)
        offset += titleBa.size
        res[offset++] = 0xF2.toByte()
        res[offset++] = usernameBa.size.toByte()
        System.arraycopy(usernameBa, 0, res, offset, usernameBa.size)
        offset += usernameBa.size
        res[offset++] = 0xF3.toByte()
        res[offset++] = passwordBa.size.toByte()
        System.arraycopy(passwordBa, 0, res, offset, passwordBa.size)
        return res
    }

    /**
     * Build update password entry payload.
     *
     * @param oldTitle former password title
     * @param title new password title
     * @param username new username value
     * @param password new password value
     * @return data payload
     */
    @JvmStatic
    fun buildEditPassword(oldTitle: String, title: String, username: String, password: String): ByteArray {
        val oldTitleBa = oldTitle.toByteArray()
        val titleBa = title.toByteArray()
        val usernameBa = username.toByteArray()
        val passwordBa = password.toByteArray()

        val res = ByteArray(oldTitleBa.size + titleBa.size + usernameBa.size + passwordBa.size + 4 + 4)

        var offset = 0

        res[offset++] = 0xF4.toByte()
        res[offset++] = oldTitleBa.size.toByte()
        System.arraycopy(oldTitleBa, 0, res, offset, oldTitleBa.size)
        offset += oldTitleBa.size

        res[offset++] = 0xF1.toByte()
        res[offset++] = titleBa.size.toByte()
        System.arraycopy(titleBa, 0, res, offset, titleBa.size)
        offset += titleBa.size

        res[offset++] = 0xF2.toByte()
        res[offset++] = usernameBa.size.toByte()
        System.arraycopy(usernameBa, 0, res, offset, usernameBa.size)
        offset += usernameBa.size

        res[offset++] = 0xF3.toByte()
        res[offset++] = passwordBa.size.toByte()
        System.arraycopy(passwordBa, 0, res, offset, passwordBa.size)

        return res
    }

    /**
     * Parse GET password list response.
     *
     * @param data data payload
     * @return list of password entry
     */
    @JvmStatic
    fun parsePaswordList(data: ByteArray): List<Password> {
        var state = 0
        var currentTag: ByteArray? = null
        var index = 0
        val passwordList = mutableListOf<Password>()

        for (i in data.indices) {
            when (state) {
                0 -> {
                    if ((data[i].toInt() and 0xFF) == 0xF1) {
                        state = 1
                    }
                }
                1 -> {
                    currentTag = ByteArray(data[i].toInt() and 0xFF)
                    state = 2
                    index = 0
                }
                2 -> {
                    currentTag!![index++] = data[i]
                    if (index == currentTag.size) {
                        state = 0
                        passwordList.add(Password(String(currentTag), null, null))
                    }
                }
            }
        }
        return passwordList
    }

    /**
     * Build delete password entry data payload.
     *
     * @param title password title
     * @return data payload
     */
    @JvmStatic
    fun buildDeletePassword(title: String): ByteArray {
        val titleBa = title.toByteArray()

        val res = ByteArray(titleBa.size + 2)
        res[0] = 0xF1.toByte()
        res[1] = titleBa.size.toByte()
        System.arraycopy(titleBa, 0, res, 2, titleBa.size)
        return res
    }

    /**
     * Build get password entry data payload.
     *
     * @param title password title
     * @return data payload
     */
    @JvmStatic
    fun buildGetPassword(title: String): ByteArray {
        val titleBa = title.toByteArray()

        val res = ByteArray(titleBa.size + 2)
        res[0] = 0xF1.toByte()
        res[1] = titleBa.size.toByte()
        System.arraycopy(titleBa, 0, res, 2, titleBa.size)
        return res
    }

    /**
     * Parse Get password entry response.
     *
     * @param title password title (from the request)
     * @param data data payload
     * @return password entry object
     */
    @JvmStatic
    fun parsePassword(title: String, data: ByteArray): Password? {
        var state = 0
        var currentTag: ByteArray? = null
        var index = 0
        var username: String? = null
        var password: ByteArray? = null

        for (i in data.indices) {
            when (state) {
                0 -> {
                    if ((data[i].toInt() and 0xFF) == 0xF2) {
                        state = 1
                    }
                }
                1 -> {
                    currentTag = ByteArray(data[i].toInt() and 0xFF)
                    state = 2
                    index = 0
                }
                2 -> {
                    currentTag!![index++] = data[i]
                    if (index == currentTag.size) {
                        state = 3
                        username = String(currentTag)
                    }
                }
                3 -> {
                    if ((data[i].toInt() and 0xFF) == 0xF3) {
                        state = 4
                    }
                }
                4 -> {
                    currentTag = ByteArray(data[i].toInt() and 0xFF)
                    state = 5
                    index = 0
                }
                5 -> {
                    currentTag!![index++] = data[i]
                    if (index == currentTag.size) {
                        state = 0
                        password = currentTag
                    }
                }
            }
        }
        return if (username != null && password != null) {
            Password(title, username, password)
        } else null
    }

    /**
     * Convert string to byte array with pin code values.
     */
    @JvmStatic
    fun convertPinCode(pass: String): ByteArray? {
        if (isNumeric(pass)) {
            val data = ByteArray(pass.length)
            for (i in pass.indices) {
                data[i] = (pass[i].code - 48).toByte()
            }
            return data
        }
        return null
    }

    /**
     * https://stackoverflow.com/a/1102916/2614364.
     */
    @JvmStatic
    fun isNumeric(str: String): Boolean {
        return str.matches(Regex("-?\\d+(\\.\\d+)?"))  //match a number with optional '-' and decimal.
    }
}
