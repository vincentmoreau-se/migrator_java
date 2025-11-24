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

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import fr.bmartel.smartcard.passwordwallet.BaseActivity
import fr.bmartel.smartcard.passwordwallet.R
import fr.bmartel.smartcard.passwordwallet.adapter.PasswordAdapter
import fr.bmartel.smartcard.passwordwallet.common.SimpleDividerItemDecoration
import fr.bmartel.smartcard.passwordwallet.dialog.ModeDialog
import fr.bmartel.smartcard.passwordwallet.inter.IDeletionListener
import fr.bmartel.smartcard.passwordwallet.inter.IFragmentOptions
import fr.bmartel.smartcard.passwordwallet.inter.IViewHolderClickListener

/**
 * Password Fragment.
 *
 * @author Bertrand Martel
 */
class PasswordFragment : ListFragmentAbstr(), IFragmentOptions {

    private var mFragment: PasswordItemFragment? = null
    private var mActivity: FragmentActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.password_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setDeletionListener()

        mEmptyFrame = view.findViewById(R.id.waiting_frame)
        mDisplayFrame = view.findViewById(R.id.display_frame)

        if (getRootActivity().getPasswordList().size > 0) {
            mEmptyFrame.visibility = View.GONE
            mDisplayFrame.visibility = View.VISIBLE
        }

        mPasswordListView = view.findViewById(R.id.password_list)
        mPasswordList = (requireActivity() as BaseActivity).getPasswordList()

        // Sort by title
        if (mPasswordList.size > 0) {
            mPasswordList = mPasswordList.sortedBy { it.title }
        }

        mPasswordAdapter = PasswordAdapter(
            getRootActivity(),
            mPasswordList,
            requireActivity(),
            IViewHolderClickListener { v ->
                val index = mPasswordListView.getChildAdapterPosition(v)
                if (!mPasswordAdapter.isSelected(index)) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        mFragment = PasswordItemFragment()
                        val args = Bundle()
                        args.putInt("index", index)
                        mFragment!!.arguments = args

                        val ft = mActivity!!.supportFragmentManager.beginTransaction()
                        ft.replace(R.id.fragment_frame, mFragment!!, "PasswordItem")
                        ft.addToBackStack(null)
                        ft.commit()
                    }, 200)
                } else {
                    mPasswordAdapter.unselect()
                }
            }
        )

        // Set layout manager
        mPasswordListView.layoutManager = GridLayoutManager(
            requireActivity(),
            1,
            LinearLayoutManager.VERTICAL,
            false
        )

        // Set line decoration
        mPasswordListView.addItemDecoration(
            SimpleDividerItemDecoration(requireActivity().applicationContext)
        )

        mPasswordListView.adapter = mPasswordAdapter

        // Setup swipe refresh
        mSwipeRefreshLayout = view.findViewById(R.id.swiperefresh)
        mSwipeRefreshLayout.setOnRefreshListener {
            mPasswordList = (requireActivity() as BaseActivity).getPasswordList()
            mPasswordAdapter.notifyDataSetChanged()
            mSwipeRefreshLayout.isRefreshing = false
        }

        onUpdateToolbar()
    }

    override fun onUpdateToolbar() {
        val buttonCreate = getRootActivity().getToolbar().menu.findItem(R.id.button_add_password)
        buttonCreate.isVisible = true
        buttonCreate.setOnMenuItemClickListener {
            val ft = parentFragmentManager.beginTransaction()
            mFragment = PasswordItemFragment()
            ft.replace(R.id.fragment_frame, mFragment!!, "PasswordItem")
            ft.addToBackStack(null)
            ft.commit()
            false
        }

        val buttonMode = getRootActivity().getToolbar().menu.findItem(R.id.button_mode)
        buttonMode.isVisible = true
        buttonMode.setOnMenuItemClickListener {
            val dialog = ModeDialog(requireActivity())
            getRootActivity().setCurrentDialog(dialog)
            dialog.show()
            false
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            mActivity = context as FragmentActivity
        }
    }

    override fun onResume() {
        super.onResume()
        setDeletionListener()
        setTitle(getString(R.string.title_password))

        getRootActivity().hideMenuButton()
        getRootActivity().getToolbar().menu.findItem(R.id.button_mode).isVisible = true
        getRootActivity().getToolbar().menu.findItem(R.id.button_add_password).isVisible = true

        mPasswordList = (requireActivity() as BaseActivity).getPasswordList()
        mPasswordAdapter.notifyDataSetChanged()
    }

    private fun setDeletionListener() {
        getRootActivity().setDeletionListener(object : IDeletionListener {
            override fun onDelete() {
                getRootActivity().deletePassword(mPasswordList[mPasswordAdapter.getSelectedItem()].title ?: "")
                mPasswordList = (requireActivity() as BaseActivity).getPasswordList()
                mPasswordAdapter.notifyDataSetChanged()

                if (getRootActivity().getPasswordList().size > 0) {
                    mEmptyFrame.visibility = View.GONE
                    mDisplayFrame.visibility = View.VISIBLE
                } else {
                    mEmptyFrame.visibility = View.VISIBLE
                    mDisplayFrame.visibility = View.GONE
                }
                getRootActivity().getToolbar().menu.findItem(R.id.button_delete).isVisible = false
            }
        })
    }

    override fun onPause() {
        super.onPause()
    }
}
