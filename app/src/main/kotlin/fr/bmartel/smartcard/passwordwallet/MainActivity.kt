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
package fr.bmartel.smartcard.passwordwallet

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import fr.bmartel.smartcard.passwordwallet.application.PasswordApplication
import fr.bmartel.smartcard.passwordwallet.auth.BiometricAuthManager
import fr.bmartel.smartcard.passwordwallet.fragment.PasswordFragment
import fr.bmartel.smartcard.passwordwallet.inter.IServiceConnection

/**
 * Main activity.
 *
 * @author Bertrand Martel
 */
class MainActivity : BaseActivity() {

    /**
     * one dialog to show above the activity. We dont want to have multiple Dialog above each other.
     */
    private var mDialog: Dialog? = null

    private lateinit var mApplication: PasswordApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        setLayout(R.layout.activity_main)
        super.onCreate(savedInstanceState)

        mApplication = application as PasswordApplication

        if (mApplication.isConnected()) {
            unlock()
        } else {
            mApplication.setServiceConnection(object : IServiceConnection {
                override fun onServiceConnected() {
                    unlock()
                }
            })
        }
    }

    /**
     * unlock activity using androidx.biometric authentication
     */
    private fun unlock() {
        val biometricAuthManager = BiometricAuthManager(this)

        if (biometricAuthManager.isBiometricAvailable() || biometricAuthManager.isDeviceCredentialEnrolled()) {
            biometricAuthManager.authenticate(object : BiometricAuthManager.AuthenticationCallback {
                override fun onAuthenticationSucceeded() {
                    Log.i(TAG, "Biometric authentication succeeded")
                    onCorrectPinCode()
                }

                override fun onAuthenticationFailed() {
                    Log.w(TAG, "Biometric authentication failed")
                    Toast.makeText(
                        this@MainActivity,
                        "Authentication failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                    Log.e(TAG, "Biometric authentication error: $errorCode - $errorMessage")
                    if (errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                        errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                        // User cancelled, close the app
                        finish()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Authentication error: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            })
        } else {
            // No biometric or device credentials available, proceed without authentication
            Log.w(TAG, "No authentication method available, proceeding without authentication")
            Toast.makeText(
                this,
                "No biometric or device lock configured. Please set up device security.",
                Toast.LENGTH_LONG
            ).show()
            onCorrectPinCode()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // No longer needed for biometric authentication
    }

    override fun onReady() {
        mFragment = PasswordFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_frame, mFragment!!)
            .commit()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return true
    }

    override fun setCurrentDialog(dialog: Dialog?) {
        mDialog = dialog
    }

    override fun onDestroy() {
        super.onDestroy()
        mDialog?.dismiss()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
