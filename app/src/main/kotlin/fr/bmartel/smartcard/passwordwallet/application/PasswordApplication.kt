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
package fr.bmartel.smartcard.passwordwallet.application

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import fr.bmartel.smartcard.passwordwallet.R
import fr.bmartel.smartcard.passwordwallet.inter.IServiceConnection
import fr.bmartel.smartcard.passwordwallet.uicc.Uicc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.simalliance.openmobileapi.SEService
import java.io.IOException

/**
 * Password Application which bounds to SEService.
 *
 * @author Bertrand Martel
 */
class PasswordApplication : Application(), SEService.CallBack {

    /**
     * SEService used to interact with SmartCard API.
     */
    private lateinit var seService: SEService

    /**
     * UICC object used to manage UICC I/O.
     */
    private lateinit var mUicc: Uicc

    /**
     * Application coroutine scope.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * working mode.
     */
    var mode: Byte = 0x00

    /**
     * service connection object.
     */
    private var mServiceConnection: IServiceConnection? = null

    private var connected = false

    private lateinit var mHandler: Handler

    override fun onCreate() {
        super.onCreate()

        mHandler = Handler(Looper.getMainLooper())

        initSeService()
    }

    override fun onTerminate() {
        applicationScope.cancel()
        mUicc.closeChannel()
        if (seService.isConnected) {
            seService.shutdown()
        }
        super.onTerminate()
    }

    fun setServiceConnection(callback: IServiceConnection) {
        mServiceConnection = callback
    }

    /**
     * Bind to SEService.
     */
    private fun initSeService() {
        try {
            Log.v(TAG, "creating SEService object")
            seService = SEService(this, this)
            mUicc = Uicc(seService)
        } catch (e: SecurityException) {
            Log.e(TAG, "Binding not allowed, uses-permission org.simalliance.openmobileapi.SMARTCARD?")
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
        }
    }

    /**
     * Connection callback for SEService.
     *
     * @param service
     */
    override fun serviceConnected(service: SEService) {
        Log.v(TAG, "serviceConnected()")
        applicationScope.launch(Dispatchers.IO) {
            try {
                // open logical channel
                mUicc.openChannel()
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException", e)
                withContext(Dispatchers.Main) {
                    if (e.message?.contains("no APDU access allowed") == true) {
                        Toast.makeText(
                            this@PasswordApplication,
                            "Application not authorized.\nCheck Access Control Rules on this card",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@PasswordApplication,
                            "SIM card not inserted",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "IOException", e)
                withContext(Dispatchers.Main) {
                    if (e.message?.contains("iccOpenLogicalChannel failed") == true) {
                        Toast.makeText(
                            this@PasswordApplication,
                            "smartcard has been disconnected or applet not installed ?",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@PasswordApplication,
                            "SIM card not inserted",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            withContext(Dispatchers.Main) {
                mServiceConnection?.onServiceConnected()
                connected = true
            }
        }
    }

    /**
     * Get the current mode from UICC.
     */
    fun refreshMode() {
        val modeRes = mUicc.getMode()
        if (!modeRes.isSuccessful()) {
            Log.e(TAG, "get mode failed")
        } else {
            mode = modeRes.getData()[0]
        }
    }

    fun getUicc(): Uicc {
        return mUicc
    }

    fun getApplicationScope(): CoroutineScope {
        return applicationScope
    }

    /**
     * Check if card is secured from UICC (eg if the pin code has been already set before).
     *
     * @return
     */
    fun isCardSecured(): Boolean {
        val cardStateRes = mUicc.getCardState()
        if (cardStateRes.getData().isNotEmpty()) {
            return cardStateRes.getData()[0] == CARD_SECURED
        }
        return true
    }

    fun isPinCodeChecked(): Boolean {
        return mUicc.getPinCodeState().isSuccessful()
    }

    fun isConnected(): Boolean {
        return connected
    }

    companion object {
        private val TAG = PasswordApplication::class.java.simpleName

        // store encrypted passwords on application or on SIM card
        const val MODE_APP_STORAGE: Byte = 0x01
        const val MODE_SIM_STORAGE: Byte = 0x02

        /**
         * Value for Global Platform secured state.
         */
        const val CARD_SECURED: Byte = 15
    }
}
