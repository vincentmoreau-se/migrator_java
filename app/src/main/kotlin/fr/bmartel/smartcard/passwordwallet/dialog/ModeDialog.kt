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
package fr.bmartel.smartcard.passwordwallet.dialog

import android.app.Activity
import android.app.Dialog
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import fr.bmartel.smartcard.passwordwallet.R
import fr.bmartel.smartcard.passwordwallet.application.PasswordApplication
import fr.bmartel.smartcard.passwordwallet.inter.IBaseActivity
import fr.bmartel.smartcard.passwordwallet.inter.ICompletionListener

/**
 * Mode dialog.
 *
 * @author Bertrand Martel
 */
class ModeDialog(rootActivity: Activity) : Dialog(rootActivity) {

    init {
        setContentView(R.layout.mode_dialog)

        window?.let { win ->
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(win.attributes)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            win.attributes = lp
        }

        val simRadio = findViewById<RadioButton>(R.id.radio_sim)
        val appRadio = findViewById<RadioButton>(R.id.radio_app)
        val transferProgress = findViewById<ProgressBar>(R.id.transferProgress)

        val activity = rootActivity as IBaseActivity
        val app = rootActivity.application as PasswordApplication

        when (app.mode) {
            PasswordApplication.MODE_APP_STORAGE -> appRadio.isChecked = true
            PasswordApplication.MODE_SIM_STORAGE -> simRadio.isChecked = true
        }

        setTitle(R.string.mode_title)

        val button = findViewById<Button>(R.id.confirm)

        button.setOnClickListener {
            button.isEnabled = false

            if ((appRadio.isChecked && app.mode == PasswordApplication.MODE_APP_STORAGE) ||
                (simRadio.isChecked && app.mode == PasswordApplication.MODE_SIM_STORAGE)) {
                cancel()
            }

            if (appRadio.isChecked && app.mode != PasswordApplication.MODE_APP_STORAGE) {
                activity.setMode(PasswordApplication.MODE_APP_STORAGE, transferProgress, object : ICompletionListener {
                    override fun onComplete() {
                        cancel()
                    }
                })
            } else if (simRadio.isChecked && app.mode != PasswordApplication.MODE_SIM_STORAGE) {
                activity.setMode(PasswordApplication.MODE_SIM_STORAGE, transferProgress, object : ICompletionListener {
                    override fun onComplete() {
                        cancel()
                    }
                })
            }
        }
    }
}
