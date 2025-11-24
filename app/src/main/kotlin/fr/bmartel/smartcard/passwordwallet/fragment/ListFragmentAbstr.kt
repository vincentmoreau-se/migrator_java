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
package fr.bmartel.smartcard.passwordwallet.fragment

import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import fr.bmartel.smartcard.passwordwallet.adapter.PasswordAdapter
import fr.bmartel.smartcard.passwordwallet.model.Password

/**
 * Common fragment.
 *
 * @author Bertrand Martel
 */
abstract class ListFragmentAbstr : MainFragmentAbstr() {

    protected lateinit var mPasswordListView: RecyclerView
    protected lateinit var mPasswordAdapter: PasswordAdapter
    protected lateinit var mPasswordList: List<Password>
    protected lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    protected lateinit var mEmptyFrame: FrameLayout
    protected lateinit var mDisplayFrame: RelativeLayout

    protected fun setTitle(title: String) {
        getRootActivity().setToolbarTitle(title)
    }
}
