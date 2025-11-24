package fr.bmartel.smartcard.passwordwallet.utils

/**
 * Some functions used to manage hex
 *
 * @author Bertrand Martel
 */
object HexUtils {

    private val hexArray = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    /**
     * Convert hex string to byte array
     * https://stackoverflow.com/a/18714790/2614364
     */
    @JvmStatic
    fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)

        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    /**
     * Convert byte array to hex string
     * https://stackoverflow.com/a/18714790/2614364
     */
    @JvmStatic
    fun byteArrayToHexString(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)

        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
