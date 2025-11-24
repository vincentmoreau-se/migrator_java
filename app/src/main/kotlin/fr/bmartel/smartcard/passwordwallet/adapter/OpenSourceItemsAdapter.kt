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
package fr.bmartel.smartcard.passwordwallet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import fr.bmartel.smartcard.passwordwallet.R

/**
 * Adapter for open source projects
 *
 * @author Bertrand Martel
 */
class OpenSourceItemsAdapter(context: Context) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = COMPONENTS.size

    override fun getItem(position: Int): Any = COMPONENTS[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.open_source_items, parent, false)

        val title = view.findViewById<TextView>(R.id.title)
        val url = view.findViewById<TextView>(R.id.url)

        title.text = COMPONENTS[position][0]
        url.text = COMPONENTS[position][1]

        return view
    }

    companion object {
        private val COMPONENTS = arrayOf(
            arrayOf("Lollipin", "https://github.com/omadahealth/LolliPin"),
            arrayOf("javacard tutorial", "https://github.com/bertrandmartel/javacard-tutorial"),
            arrayOf("(dev) pcsc emulator", "https://github.com/bertrandmartel/pcsc-android-emulator"),
            arrayOf("(dev) JavaCard Gradle plugin", "https://github.com/bertrandmartel/javacard-gradle-plugin"),
            arrayOf("(dev) seek for Android", " https://github.com/seek-for-android/pool")
        )
    }
}
