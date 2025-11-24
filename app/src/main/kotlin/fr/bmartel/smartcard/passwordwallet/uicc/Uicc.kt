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

import android.util.Log
import fr.bmartel.smartcard.passwordwallet.model.Password
import fr.bmartel.smartcard.passwordwallet.utils.HexUtils
import org.simalliance.openmobileapi.Channel
import org.simalliance.openmobileapi.SEService
import java.io.IOException

/**
 * Manage all I/O with UICC.
 *
 * @author Bertrand Martel
 */
class Uicc(private val service: SEService) {

    private var channel: Channel? = null

    /**
     * Open logical channel with applet.
     *
     * @throws SecurityException
     * @throws IOException
     */
    @Throws(SecurityException::class, IOException::class)
    fun openChannel() {
        val readers = service.readers
        if (readers.isEmpty()) return

        val session = readers[0].openSession()
        channel = session.openLogicalChannel(HexUtils.hexStringToByteArray(APPLET_ID))
    }

    /**
     * Send APDU to UICC.
     *
     * @param data data payload
     * @param operation instruction
     * @return APDU response object
     */
    private fun requestSE(data: ByteArray, operation: Byte): ApduResponse {
        return requestSE(data, 0x00.toByte(), 0x00.toByte(), operation)
    }

    /**
     * Send APDU to UICC.
     *
     * @param data data payload
     * @param p1 P1 param
     * @param p2 P2 param
     * @param operation instruction
     * @return APDU response object
     */
    private fun requestSE(data: ByteArray, p1: Byte, p2: Byte, operation: Byte): ApduResponse {
        channel?.let { ch ->
            try {
                val respApdu = if (data.isNotEmpty()) {
                    ch.transmit(CommandApdu(0x90.toByte().toInt(), operation.toInt(), p1.toInt(), p2.toInt(), data).toBytes())
                } else {
                    ch.transmit(CommandApdu(0x90.toByte().toInt(), operation.toInt(), p1.toInt(), p2.toInt(), 0x00).toBytes())
                }
                return ApduResponse(respApdu)
            } catch (e: Exception) {
                Log.e(TAG, "Error occurred:", e)
            }
        }
        return ApduResponse(byteArrayOf())
    }

    /**
     * Get password list.
     */
    fun getPasswordList(): List<Password>? {
        val result = requestSE(byteArrayOf(), INS_LIST_PASSWORD)

        return if (result.isSuccessful()) {
            UiccUtils.parsePaswordList(result.getData())
        } else null
    }

    /**
     * Encrypt data on UICC.
     *
     * @param data clear text data
     * @return APDU response
     */
    fun encrypt(data: ByteArray): ApduResponse {
        return requestSE(data, INS_ENCRYPT)
    }

    /**
     * Decrypt data on UICC.
     *
     * @param data encrypted data
     * @return clear text data
     */
    fun decrypt(data: ByteArray): ApduResponse {
        return requestSE(data, INS_DECRYPT)
    }

    /**
     * Update a password entry.
     *
     * @param formerTitle former password title
     * @param newTitle new password title
     * @param username updated username
     * @param password updated password value
     * @return APDU response
     */
    fun editPassword(formerTitle: String, newTitle: String, username: String, password: String): ApduResponse {
        return requestSE(UiccUtils.buildEditPassword(formerTitle, newTitle, username, password), INS_EDIT_PASSWORD)
    }

    /**
     * Add a new password entry.
     *
     * @param title password title
     * @param username username
     * @param password password value
     * @return APDU response
     */
    fun addPassword(title: String, username: String, password: String): ApduResponse {
        return requestSE(UiccUtils.buildAddPassword(title, username, password), INS_ADD_PASSWORD)
    }

    /**
     * Get password entry from password title.
     *
     * @param title password title
     * @return APDU response
     */
    fun getPassword(title: String): ApduResponse {
        return requestSE(UiccUtils.buildGetPassword(title), INS_GET_PASSWORD)
    }

    /**
     * Delete password entry.
     *
     * @param title password title
     * @return APDU response
     */
    fun deletePassword(title: String): ApduResponse {
        return requestSE(UiccUtils.buildDeletePassword(title), INS_DELETE_PASSWORD)
    }

    /**
     * Set working mode (storage on app or on UICC).
     *
     * @param mode mode value
     * @return APDU response
     */
    fun setMode(mode: Byte): ApduResponse {
        return requestSE(byteArrayOf(mode), INS_SET_MODE)
    }

    /**
     * Get working mode
     *
     * @return mode
     */
    fun getMode(): ApduResponse {
        return requestSE(byteArrayOf(), INS_GET_MODE)
    }

    /**
     * Get the card state (secured mean the pin code has already been set).
     *
     * @return GP card state
     */
    fun getCardState(): ApduResponse {
        return requestSE(byteArrayOf(), INS_GET_STATE)
    }

    /**
     * Check if pin code is already checked for this session.
     *
     * @return APDU response
     */
    fun getPinCodeState(): ApduResponse {
        return requestSE(byteArrayOf(), INS_PIN_CHECK)
    }

    /**
     * Check pin code.
     *
     * @param data pincode
     * @return pin code result
     */
    fun checkPin(data: ByteArray): PinCodeResult {
        val res = requestSE(data, 0x00.toByte(), 0x80.toByte(), INS_VERIFY)

        return if (res.isSuccessful()) {
            PinCodeResult(true, -1)
        } else {
            if ((res.getStatus().toInt() and 0xFFF0) != 0x63C0) {
                PinCodeResult(false, 0)
            } else {
                PinCodeResult(false, res.getStatus().toInt() and 0x000F)
            }
        }
    }

    /**
     * Update pin code.
     *
     * @param noPin true if it's the first time we set pin code
     * @param oldPin old pin code (if noPin is false)
     * @param newPin new pin code
     * @return APDU response
     */
    fun updatePin(noPin: Boolean, oldPin: ByteArray?, newPin: ByteArray): ApduResponse {
        return if (noPin) {
            val request = ByteArray(newPin.size + 1)
            request[0] = newPin.size.toByte()
            System.arraycopy(newPin, 0, request, 1, newPin.size)
            requestSE(request, 0x00.toByte(), 0x80.toByte(), INS_CHANGE_REFERENCE_DATA)
        } else {
            oldPin?.let { old ->
                val request = ByteArray(old.size + 1 + newPin.size + 1)
                var index = 0
                request[index++] = old.size.toByte()
                System.arraycopy(old, 0, request, index, old.size)
                index += old.size
                request[index++] = newPin.size.toByte()
                System.arraycopy(newPin, 0, request, index, newPin.size)
                requestSE(request, 0x01.toByte(), 0x80.toByte(), INS_CHANGE_REFERENCE_DATA)
            } ?: ApduResponse(byteArrayOf())
        }
    }

    /**
     * Close current session.
     */
    fun closeChannel() {
        channel?.close()
    }

    companion object {
        private val TAG = Uicc::class.java.simpleName

        private const val APPLET_ID = "D2760001180002FF49502589C0019B01"

        private const val INS_ENCRYPT: Byte = 0x10
        private const val INS_DECRYPT: Byte = 0x11
        private const val INS_GET_MODE: Byte = 0x40
        private const val INS_SET_MODE: Byte = 0x41
        private const val INS_ADD_PASSWORD: Byte = 0x30
        private const val INS_LIST_PASSWORD: Byte = 0x36
        private const val INS_DELETE_PASSWORD: Byte = 0x34
        private const val INS_GET_PASSWORD: Byte = 0x32
        private const val INS_EDIT_PASSWORD: Byte = 0x33
        private const val INS_VERIFY: Byte = 0x20
        private const val INS_GET_STATE: Byte = 0x50
        private const val INS_PIN_CHECK: Byte = 0x51
        private const val INS_CHANGE_REFERENCE_DATA: Byte = 0x24
    }
}
