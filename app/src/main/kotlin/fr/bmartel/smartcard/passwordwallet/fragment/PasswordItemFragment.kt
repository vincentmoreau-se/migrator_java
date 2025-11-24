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

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import fr.bmartel.smartcard.passwordwallet.R
import fr.bmartel.smartcard.passwordwallet.application.PasswordApplication
import fr.bmartel.smartcard.passwordwallet.db.PasswordReaderDbHelper
import fr.bmartel.smartcard.passwordwallet.inter.IFragmentOptions
import fr.bmartel.smartcard.passwordwallet.utils.HexUtils

/**
 * Password item Fragment.
 *
 * @author Bertrand Martel
 */
class PasswordItemFragment : MainFragmentAbstr(), IFragmentOptions {

    private lateinit var mPasswordTitleEt: EditText
    private lateinit var mPasswordUsernameEt: EditText
    private lateinit var mDecryptedPasswordEt: EditText
    private lateinit var mEncryptedPasswordTv: TextView
    private lateinit var mTextObfuscationToggle: CheckBox

    private var mPasswordIndex = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.password_item_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        mPasswordIndex = args?.getInt("index", -1) ?: -1

        mPasswordTitleEt = view.findViewById(R.id.password_title)
        mPasswordTitleEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        mPasswordUsernameEt = view.findViewById(R.id.password_username)
        mPasswordUsernameEt.inputType = InputType.TYPE_CLASS_TEXT

        mDecryptedPasswordEt = view.findViewById(R.id.password_decrypted)
        mEncryptedPasswordTv = view.findViewById(R.id.password_encrypted)

        mTextObfuscationToggle = view.findViewById(R.id.text_obfuscation_toggle)

        val app = requireActivity().application as PasswordApplication

        if (mPasswordIndex == -1) {
            mEncryptedPasswordTv.visibility = View.GONE
            mTextObfuscationToggle.setText(R.string.text_obfuscation_toggle_caption)
        } else {
            mEncryptedPasswordTv.visibility = View.VISIBLE
            mTextObfuscationToggle.setText(R.string.text_obfuscation_toggle_caption_decrypt)
        }

        mTextObfuscationToggle.setOnClickListener {
            val hidePassword = mTextObfuscationToggle.isChecked
            if (!hidePassword) {
                mDecryptedPasswordEt.transformationMethod = PasswordTransformationMethod()
            } else {
                mDecryptedPasswordEt.transformationMethod = null
            }
        }

        mTextObfuscationToggle.isChecked = false

        if (mTextObfuscationToggle.isChecked) {
            mDecryptedPasswordEt.transformationMethod = PasswordTransformationMethod()
        }

        if (mPasswordIndex != -1) {
            getRootActivity().setToolbarTitle(getString(R.string.title_edit_password))
            val password = getRootActivity().getPassword(mPasswordIndex)

            if (password == null) {
                return
            }

            mPasswordTitleEt.setText(password.title)
            mPasswordUsernameEt.setText(password.username)

            if (app.mode == PasswordApplication.MODE_APP_STORAGE) {
                mEncryptedPasswordTv.text = HexUtils.byteArrayToHexString(password.password ?: byteArrayOf())

                val pass = getRootActivity().decrypt(password.password ?: byteArrayOf())
                if (pass != null) {
                    mDecryptedPasswordEt.setText(pass)
                } else {
                    Toast.makeText(requireActivity(), "password decryption failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                mDecryptedPasswordEt.setText(String(password.password ?: byteArrayOf()))
            }
        } else {
            getRootActivity().setToolbarTitle(getString(R.string.title_create_password))
        }
        onUpdateToolbar()
    }

    override fun onUpdateToolbar() {
        getRootActivity().hideMenuButton()
        val toolbar = getRootActivity().getToolbar()
        val saveButton = toolbar.menu.findItem(R.id.button_save)
        val deleteButton = toolbar.menu.findItem(R.id.button_delete)
        saveButton.isVisible = true
        deleteButton.isVisible = false

        saveButton.setOnMenuItemClickListener {
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)

            val title = mPasswordTitleEt.text.toString().trim()
            val username = mPasswordUsernameEt.text.toString().trim()
            val password = mDecryptedPasswordEt.text.toString().trim()

            when {
                title.isEmpty() -> {
                    Toast.makeText(requireActivity(), "title can't be empty", Toast.LENGTH_SHORT).show()
                    return@setOnMenuItemClickListener false
                }
                username.isEmpty() -> {
                    Toast.makeText(requireActivity(), "username can't be empty", Toast.LENGTH_SHORT).show()
                    return@setOnMenuItemClickListener false
                }
                password.isEmpty() -> {
                    Toast.makeText(requireActivity(), "password value can't be empty", Toast.LENGTH_SHORT).show()
                    return@setOnMenuItemClickListener false
                }
                title.length > PasswordReaderDbHelper.TITLE_MAX_SIZE -> {
                    Toast.makeText(requireActivity(), "title max length is ${PasswordReaderDbHelper.TITLE_MAX_SIZE} characters", Toast.LENGTH_SHORT).show()
                    return@setOnMenuItemClickListener false
                }
                username.length > PasswordReaderDbHelper.USERNAME_MAX_SIZE -> {
                    Toast.makeText(requireActivity(), "username max length is ${PasswordReaderDbHelper.USERNAME_MAX_SIZE} characters", Toast.LENGTH_SHORT).show()
                    return@setOnMenuItemClickListener false
                }
                password.length > PasswordReaderDbHelper.PASSWORD_MAX_SIZE -> {
                    Toast.makeText(requireActivity(), "password max length is ${PasswordReaderDbHelper.PASSWORD_MAX_SIZE} characters", Toast.LENGTH_SHORT).show()
                    return@setOnMenuItemClickListener false
                }
            }

            if (mPasswordIndex == -1 && getRootActivity().checkDuplicatePassword(title)) {
                Toast.makeText(requireActivity(), "password title $title already exist", Toast.LENGTH_SHORT).show()
            } else if (mPasswordIndex == -1) {
                val res = getRootActivity().saveNewPassword(title, username, password)
                if (res != null) {
                    Toast.makeText(requireActivity(), "password $title has been saved", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireActivity(), "operation failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                val formerTitle = getRootActivity().getPasswordList()[mPasswordIndex].title ?: ""
                getRootActivity().getPasswordList()[mPasswordIndex].title = title
                getRootActivity().getPasswordList()[mPasswordIndex].username = username
                val res = getRootActivity().saveExistingPassword(formerTitle, title, username, password)
                if (res != null) {
                    getRootActivity().getPasswordList()[mPasswordIndex].password = res
                    Toast.makeText(requireActivity(), "password $title has been saved", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireActivity(), "operation failed", Toast.LENGTH_SHORT).show()
                }
            }
            false
        }
    }
}
