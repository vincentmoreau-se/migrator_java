package fr.bmartel.smartcard.passwordwallet.uicc

/**
 * APDU Response object.
 */
class ApduResponse(private val data: ByteArray) {

    /**
     * Check if state is successful (0x9000 received).
     *
     * @return success state
     */
    fun isSuccessful(): Boolean {
        if (data.size >= 2) {
            return (data[data.size - 2] == 0x90.toByte()) && data[data.size - 1] == 0x00.toByte()
        }
        return false
    }

    /**
     * Get data payload.
     */
    fun getData(): ByteArray {
        if (data.size < 2) {
            return byteArrayOf()
        }
        return data.copyOfRange(0, data.size - 2)
    }

    /**
     * Get Status word.
     */
    fun getStatus(): Short {
        if (data.size < 2) {
            return 0x0000
        }
        return (((data[data.size - 2].toInt() and 0xFF) shl 8) + (data[data.size - 1].toInt() and 0xFF)).toShort()
    }

    fun getResponse(): ByteArray = data
}
