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
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import fr.bmartel.smartcard.passwordwallet.R
import fr.bmartel.smartcard.passwordwallet.inter.IBaseActivity
import fr.bmartel.smartcard.passwordwallet.inter.IViewHolderClickListener
import fr.bmartel.smartcard.passwordwallet.model.Password

/**
 * Password Adapter
 *
 * @author Bertrand Martel
 */
class PasswordAdapter(
    private val activity: IBaseActivity,
    var passwordList: List<Password>,
    private val context: Context,
    private val listener: IViewHolderClickListener
) : RecyclerView.Adapter<PasswordAdapter.ViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.password_item, parent, false)
        return ViewHolder(v, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = passwordList[position]
        holder.passwordTitle.text = item.title ?: ""

        if (selectedPosition == position) {
            holder.itemView.setBackgroundColor(Color.parseColor("#e1e1e1"))
            activity.getToolbar().menu.findItem(R.id.button_delete).isVisible = true
        } else {
            activity.getToolbar().menu.findItem(R.id.button_delete).isVisible = false
            holder.itemView.background = ContextCompat.getDrawable(context, R.drawable.ripple)
        }

        holder.itemView.setOnLongClickListener {
            notifyItemChanged(selectedPosition)
            selectedPosition = position
            notifyItemChanged(selectedPosition)
            true
        }
    }

    fun isSelected(position: Int): Boolean {
        return selectedPosition == position
    }

    fun unselect() {
        selectedPosition = -1
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return passwordList.size
    }

    fun getSelectedItem(): Int {
        return selectedPosition
    }

    /**
     * ViewHolder for Password item
     */
    inner class ViewHolder(
        v: View,
        private val clickListener: IViewHolderClickListener
    ) : RecyclerView.ViewHolder(v), View.OnClickListener {

        val layout: LinearLayout = v.findViewById(R.id.group_layout)
        val passwordTitle: TextView = v.findViewById(R.id.password_title)

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            clickListener.onClick(v)
        }
    }
}
