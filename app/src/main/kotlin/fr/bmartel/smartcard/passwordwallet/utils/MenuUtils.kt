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
package fr.bmartel.smartcard.passwordwallet.utils

import android.app.Activity
import android.util.Log
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import fr.bmartel.smartcard.passwordwallet.R
import fr.bmartel.smartcard.passwordwallet.application.PasswordApplication
import fr.bmartel.smartcard.passwordwallet.dialog.AboutDialog
import fr.bmartel.smartcard.passwordwallet.dialog.OpenSourceItemsDialog
import fr.bmartel.smartcard.passwordwallet.inter.IDialog
import java.io.IOException

/**
 * Some functions used to manage Menu.
 *
 * @author Bertrand Martel
 */
object MenuUtils {

    private val TAG = MenuUtils::class.java.simpleName

    /**
     * Execute actions according to selected menu item.
     *
     * @param currentActivity Current activity
     * @param application Password application
     * @param menuItem MenuItem object
     * @param drawer Navigation drawer
     * @param activity Dialog interface
     */
    @JvmStatic
    fun selectDrawerItem(
        currentActivity: Activity,
        application: PasswordApplication,
        menuItem: MenuItem,
        drawer: DrawerLayout,
        activity: IDialog
    ) {
        when (menuItem.itemId) {
            R.id.close_session -> {
                // Close and reopen UICC channel to reset session
                // Biometric authentication is handled at app startup in MainActivity
                application.getUicc()?.let { uicc ->
                    uicc.closeChannel()
                    try {
                        uicc.openChannel()
                        Log.i(TAG, "Session closed and reopened successfully")
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to reopen UICC channel", e)
                    }
                }
            }
            R.id.change_pincode -> {
                // PIN code management is handled by UICC
                // This would require additional UI for PIN change
                Log.i(TAG, "PIN change functionality requires UICC PIN management UI")
                // Note: Implement PIN change dialog in future if needed
            }
            R.id.open_source_components -> {
                val dialog = OpenSourceItemsDialog(currentActivity)
                activity.setCurrentDialog(dialog)
                dialog.show()
            }
            R.id.about_app -> {
                val dialog = AboutDialog(currentActivity)
                activity.setCurrentDialog(dialog)
                dialog.show()
            }
        }
        drawer.closeDrawers()
    }
}
