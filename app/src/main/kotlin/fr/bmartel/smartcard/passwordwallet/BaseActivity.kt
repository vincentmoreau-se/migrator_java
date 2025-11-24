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

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import fr.bmartel.smartcard.passwordwallet.application.PasswordApplication
import fr.bmartel.smartcard.passwordwallet.db.PasswordReaderDbHelper
import fr.bmartel.smartcard.passwordwallet.inter.IBaseActivity
import fr.bmartel.smartcard.passwordwallet.inter.ICompletionListener
import fr.bmartel.smartcard.passwordwallet.inter.IDeletionListener
import fr.bmartel.smartcard.passwordwallet.inter.IFragmentOptions
import fr.bmartel.smartcard.passwordwallet.model.Password
import fr.bmartel.smartcard.passwordwallet.uicc.UiccUtils
import fr.bmartel.smartcard.passwordwallet.utils.MenuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shared activity.
 *
 * @author Bertrand Martel
 */
abstract class BaseActivity : AppCompatActivity(), IBaseActivity {

    private var toolbar: Toolbar? = null
    protected var mDrawer: DrawerLayout? = null

    /**
     * toggle on the hamburger button
     */
    protected lateinit var drawerToggle: ActionBarDrawerToggle

    /**
     * navigation view
     */
    protected lateinit var nvDrawer: NavigationView

    /**
     * activity layout resource id
     */
    private var layoutId = 0

    /**
     * list of password retrieved from database or from UICC.
     */
    private lateinit var mPasswordList: MutableList<Password>

    /**
     * the current fragment.
     */
    protected var mFragment: Fragment? = null

    protected lateinit var mSharedPref: SharedPreferences
    protected var mDeletionListener: IDeletionListener? = null

    private lateinit var mDbHelper: PasswordReaderDbHelper
    private lateinit var mApplication: PasswordApplication

    /**
     * set activity resource id
     *
     * @param resId
     */
    protected fun setLayout(resId: Int) {
        layoutId = resId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(layoutId)

        mApplication = application as PasswordApplication
        mDbHelper = PasswordReaderDbHelper(applicationContext)
        mSharedPref = applicationContext.getSharedPreferences(applicationContext.packageName, 0)

        // Set a Toolbar to replace the ActionBar.
        toolbar = findViewById(R.id.toolbar_item)
        setSupportActionBar(toolbar)

        setToolbarTitle()
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.inflateMenu(R.menu.toolbar_menu)

        // Find our drawer view
        mDrawer = findViewById(R.id.drawer_layout)
        drawerToggle = setupDrawerToggle()
        mDrawer?.addDrawerListener(drawerToggle)
        nvDrawer = findViewById(R.id.nvView)

        // Setup drawer view
        setupDrawerContent(nvDrawer)
    }

    /**
     * called on valid pin code.
     */
    protected fun onCorrectPinCode() {
        nvDrawer.menu.findItem(R.id.close_session).isVisible = true
        nvDrawer.menu.findItem(R.id.change_pincode).isVisible = true
        updateMode()
        initModel()
        runOnUiThread {
            onReady()
        }
    }

    /**
     * update the mode icon.
     */
    protected fun updateMode() {
        mApplication.refreshMode()
        when (mApplication.mode) {
            PasswordApplication.MODE_APP_STORAGE -> {
                runOnUiThread {
                    toolbar?.menu?.findItem(R.id.button_mode)?.setIcon(R.drawable.ic_memory)
                }
            }
            PasswordApplication.MODE_SIM_STORAGE -> {
                runOnUiThread {
                    toolbar?.menu?.findItem(R.id.button_mode)?.setIcon(R.drawable.ic_sim_card)
                }
            }
        }
    }

    /**
     * initialize model.
     */
    private fun initModel() {
        when (mApplication.mode) {
            PasswordApplication.MODE_APP_STORAGE -> {
                val db = mDbHelper.readableDatabase
                val sortOrder = "${PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_TITLE} DESC"
                val cursor = db.query(
                    PasswordReaderDbHelper.PasswordEntry.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    sortOrder
                )

                mPasswordList = mutableListOf()
                while (cursor.moveToNext()) {
                    mPasswordList.add(
                        Password(
                            cursor.getString(cursor.getColumnIndexOrThrow(PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_TITLE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_USERNAME)),
                            cursor.getBlob(cursor.getColumnIndexOrThrow(PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_PASSWORD))
                        )
                    )
                }
                cursor.close()
            }
            PasswordApplication.MODE_SIM_STORAGE -> {
                val passwordList = mApplication.getUicc().getPasswordList()
                if (passwordList != null) {
                    mPasswordList = passwordList.toMutableList()
                } else {
                    Toast.makeText(this, "failed to retrieve password on UICC", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Set toolbar title in initialization
     */
    protected fun setToolbarTitle() {
        supportActionBar?.title = resources.getString(R.string.app_title)
    }

    /**
     * setup navigation view
     *
     * @param navigationView
     */
    private fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            MenuUtils.selectDrawerItem(
                this,
                mApplication,
                menuItem,
                mDrawer!!,
                this
            )
            false
        }
    }

    /**
     * setup action drawer.
     *
     * @return
     */
    protected fun setupDrawerToggle(): ActionBarDrawerToggle {
        return object : ActionBarDrawerToggle(
            this,
            mDrawer,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
            }

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                currentFocus?.let {
                    inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
                }
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                currentFocus?.let {
                    inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                mDrawer?.openDrawer(GravityCompat.START)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()

        // Handle back button with OnBackPressedCallback
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mDrawer?.isDrawerOpen(GravityCompat.START) == true) {
                    mDrawer?.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    /**
     * Save password entry validated by user.
     *
     * @param title    password title
     * @param username username
     * @param password password value
     * @return APDU data result
     */
    override fun saveNewPassword(title: String, username: String, password: String): ByteArray? {
        when (mApplication.mode) {
            PasswordApplication.MODE_APP_STORAGE -> {
                val result = mApplication.getUicc().encrypt(password.toByteArray())
                if (result.isSuccessful()) {
                    val db = mDbHelper.writableDatabase
                    val values = ContentValues().apply {
                        put(PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_TITLE, title)
                        put(PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_USERNAME, username)
                        put(PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_PASSWORD, result.getData())
                    }
                    db.insert(PasswordReaderDbHelper.PasswordEntry.TABLE_NAME, null, values)
                    val passwordObj = Password(title, username, result.getData())
                    mPasswordList.add(passwordObj)
                    return result.getData()
                }
            }
            PasswordApplication.MODE_SIM_STORAGE -> {
                val result = mApplication.getUicc().addPassword(title, username, password)
                if (result.isSuccessful()) {
                    val passwordObj = Password(title, null, null)
                    mPasswordList.add(passwordObj)
                    return result.getData()
                } else {
                    Log.e(TAG, "write operation failed")
                }
            }
        }
        return null
    }

    /**
     * Update a password entry.
     *
     * @param formerTitle last password title
     * @param newTitle    new password title
     * @param username    updated username
     * @param password    updated password value
     * @return APDU data result
     */
    override fun saveExistingPassword(formerTitle: String, newTitle: String, username: String, password: String): ByteArray? {
        when (mApplication.mode) {
            PasswordApplication.MODE_APP_STORAGE -> {
                val result = mApplication.getUicc().encrypt(password.toByteArray())
                if (result.isSuccessful()) {
                    val db = mDbHelper.writableDatabase
                    val values = ContentValues().apply {
                        put(PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_TITLE, newTitle)
                        put(PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_USERNAME, username)
                        put(PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_PASSWORD, result.getData())
                    }
                    val whereArgs = arrayOf(formerTitle)
                    db.update(
                        PasswordReaderDbHelper.PasswordEntry.TABLE_NAME,
                        values,
                        "${PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_TITLE}=?",
                        whereArgs
                    )
                    return result.getData()
                }
            }
            PasswordApplication.MODE_SIM_STORAGE -> {
                val result = mApplication.getUicc().editPassword(formerTitle, newTitle, username, password)
                if (result.isSuccessful()) {
                    for (i in mPasswordList.indices) {
                        if (mPasswordList[i].title == formerTitle) {
                            mPasswordList[i] = Password(newTitle, null, null)
                            break
                        }
                    }
                    return result.getData()
                } else {
                    Log.e(TAG, "write operation failed")
                }
            }
        }
        return null
    }

    /**
     * decrypt password on UICC.
     *
     * @param password encrypted password
     * @return password decrypted
     */
    override fun decrypt(password: ByteArray): String? {
        val data = mApplication.getUicc().decrypt(password)
        if (data.isSuccessful()) {
            return String(data.getData())
        }
        return null
    }

    /**
     * Update the mode (storage on application or on UICC).
     *
     * @param mode     mode
     * @param progress progressbar object
     * @param listener completion listener
     */
    override fun setMode(mode: Byte, progress: ProgressBar, listener: ICompletionListener) {
        mApplication.getApplicationScope().launch(Dispatchers.IO) {
            Log.v(TAG, "set mode $mode")
            val res = mApplication.getUicc().setMode(mode)
            if (!res.isSuccessful()) {
                Log.e(TAG, "set mode failed")
            } else {
                mApplication.mode = mode

                val tempList = mutableListOf<Password>()
                // transition between modes
                when (mApplication.mode) {
                    PasswordApplication.MODE_APP_STORAGE -> {
                        for (password in mPasswordList) {
                            tempList.add(Password(password.title, password.username, password.password))
                        }
                        mPasswordList.clear()
                        for (i in tempList.indices) {
                            val result = mApplication.getUicc().getPassword(tempList[i].title ?: "")
                            if (result.isSuccessful()) {
                                val realPassword = UiccUtils.parsePassword(tempList[i].title ?: "", result.getData())
                                realPassword?.let {
                                    // add password in database
                                    saveNewPassword(it.title ?: "", it.username ?: "", String(it.password ?: byteArrayOf()))
                                    // delete on UICC
                                    mApplication.getUicc().deletePassword(it.title ?: "")
                                    withContext(Dispatchers.Main) {
                                        progress.progress = (i * 100) / tempList.size
                                    }
                                }
                            }
                        }
                        progress.progress = 100
                        withContext(Dispatchers.Main) {
                            toolbar?.menu?.findItem(R.id.button_mode)?.setIcon(R.drawable.ic_memory)
                        }
                    }
                    PasswordApplication.MODE_SIM_STORAGE -> {
                        for (password in mPasswordList) {
                            tempList.add(Password(password.title, password.username, password.password))
                        }
                        mPasswordList.clear()
                        for (i in tempList.indices) {
                            // add password in UICC
                            saveNewPassword(
                                tempList[i].title ?: "",
                                tempList[i].username ?: "",
                                decrypt(tempList[i].password ?: byteArrayOf()) ?: ""
                            )
                            withContext(Dispatchers.Main) {
                                progress.progress = (i * 100) / tempList.size
                            }
                        }
                        // drop all data from database
                        dropAll()
                        progress.progress = 100
                        withContext(Dispatchers.Main) {
                            toolbar?.menu?.findItem(R.id.button_mode)?.setIcon(R.drawable.ic_sim_card)
                        }
                    }
                }
                initModel()
            }
            listener.onComplete()
        }
    }

    /**
     * Get the password entry from local storage or from UICC.
     *
     * @param index password index in password list
     * @return password entry or null
     */
    override fun getPassword(index: Int): Password? {
        when (mApplication.mode) {
            PasswordApplication.MODE_APP_STORAGE -> {
                return mPasswordList[index]
            }
            PasswordApplication.MODE_SIM_STORAGE -> {
                val password = mPasswordList[index]
                val result = mApplication.getUicc().getPassword(password.title ?: "")
                if (result.isSuccessful()) {
                    return UiccUtils.parsePassword(password.title ?: "", result.getData())
                } else {
                    Toast.makeText(this, "failed to retrieve password on UICC", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return null
    }

    /**
     * remove all passwords from local db.
     */
    private fun dropAll() {
        val db = mDbHelper.writableDatabase
        db.execSQL("delete from ${PasswordReaderDbHelper.PasswordEntry.TABLE_NAME}")
    }

    /**
     * Delete a password entry.
     *
     * @param title password title
     */
    override fun deletePassword(title: String) {
        for (password in mPasswordList) {
            if (password.title == title) {
                when (mApplication.mode) {
                    PasswordApplication.MODE_APP_STORAGE -> {
                        val db = mDbHelper.writableDatabase
                        val whereClause = "${PasswordReaderDbHelper.PasswordEntry.COLUMN_NAME_TITLE}=?"
                        val whereArgs = arrayOf(password.title)
                        db.delete(
                            PasswordReaderDbHelper.PasswordEntry.TABLE_NAME,
                            whereClause,
                            whereArgs
                        )
                        mPasswordList.remove(password)
                    }
                    PasswordApplication.MODE_SIM_STORAGE -> {
                        val result = mApplication.getUicc().deletePassword(title)
                        if (!result.isSuccessful()) {
                            Toast.makeText(this, "failed to delete password on UICC", Toast.LENGTH_SHORT).show()
                        } else {
                            mPasswordList.remove(password)
                        }
                    }
                }
                break
            }
        }
    }

    /**
     * Check for duplicate password.
     *
     * @param title password title
     * @return true if password duplicate found
     */
    override fun checkDuplicatePassword(title: String): Boolean {
        for (password in mPasswordList) {
            if (password.title == title) {
                return true
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        hideMenuButton()

        mFragment?.let {
            (it as? IFragmentOptions)?.onUpdateToolbar()
        }

        val deleteButton = toolbar?.menu?.findItem(R.id.button_delete)
        deleteButton?.setOnMenuItemClickListener {
            mDeletionListener?.onDelete()
            false
        }
        updateMode()
        return super.onCreateOptionsMenu(menu)
    }

    override fun hideMenuButton() {
        toolbar?.menu?.findItem(R.id.button_save)?.isVisible = false
        toolbar?.menu?.findItem(R.id.button_mode)?.isVisible = false
        toolbar?.menu?.findItem(R.id.button_delete)?.isVisible = false
        toolbar?.menu?.findItem(R.id.button_add_password)?.isVisible = false
    }

    override fun getPasswordList(): MutableList<Password> {
        return mPasswordList
    }

    override fun getToolbar(): Toolbar = toolbar!!

    override fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun setDeletionListener(listener: IDeletionListener?) {
        mDeletionListener = listener
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private val TAG = BaseActivity::class.java.simpleName
    }
}
