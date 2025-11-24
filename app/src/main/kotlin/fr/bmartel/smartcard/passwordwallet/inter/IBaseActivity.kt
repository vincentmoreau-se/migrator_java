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
package fr.bmartel.smartcard.passwordwallet.inter

import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import fr.bmartel.smartcard.passwordwallet.model.Password

/**
 * Interface used for the fragment to communicate with the main activity.
 *
 * @author Bertrand Martel
 */
interface IBaseActivity : IDialog {

    /**
     * Get password list.
     */
    fun getPasswordList(): List<Password>

    /**
     * Get toolbar object.
     */
    fun getToolbar(): Toolbar

    /**
     * Set toolbar title.
     *
     * @param title The title to set
     */
    fun setToolbarTitle(title: String)

    /**
     * hide button in toolbar.
     */
    fun hideMenuButton()

    /**
     * Set the deletion listener to be called when user click on delete button.
     *
     * @param listener The deletion listener
     */
    fun setDeletionListener(listener: IDeletionListener?)

    /**
     * called when a new password should be created.
     *
     * @param title password title
     * @param username username
     * @param password password value
     * @return data payload
     */
    fun saveNewPassword(title: String, username: String, password: String): ByteArray?

    /**
     * called when a password should be updated.
     *
     * @param formerTitle former password title
     * @param newTitle new password title
     * @param username new username value
     * @param password new password value
     * @return data payload
     */
    fun saveExistingPassword(formerTitle: String, newTitle: String, username: String, password: String): ByteArray?

    /**
     * Delete password entry.
     *
     * @param title password title
     */
    fun deletePassword(title: String)

    /**
     * Check if password is duplicate.
     *
     * @param title password title
     * @return true if password is duplicated
     */
    fun checkDuplicatePassword(title: String): Boolean

    /**
     * Decrypt password.
     *
     * @param password encrypted password
     * @return clear text password
     */
    fun decrypt(password: ByteArray): String?

    /**
     * Set working mode.
     *
     * @param mode working mode
     * @param progress progress bar
     * @param listener completion listener
     */
    fun setMode(mode: Byte, progress: ProgressBar, listener: ICompletionListener)

    /**
     * called when correct pin code have been set and password fragment should be opened.
     */
    fun onReady()

    /**
     * Get password entry.
     *
     * @param index index in password list
     * @return password entry
     */
    fun getPassword(index: Int): Password?
}
