/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.bytesforge.linkasanote.manageaccounts

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.databinding.ActivityManageAccountsBinding
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsActivity
import com.bytesforge.linkasanote.utils.ActivityUtils
import javax.inject.Inject

class ManageAccountsActivity : AppCompatActivity() {
    @JvmField
    @Inject
    var presenter: ManageAccountsPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityManageAccountsBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_manage_accounts)
        // Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setTitle(R.string.actionbar_title_manage_accounts)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        // Fragment
        var fragment = supportFragmentManager
            .findFragmentById(R.id.content_frame) as ManageAccountsFragment?
        if (fragment == null) {
            fragment = ManageAccountsFragment.newInstance()
            ActivityUtils.addFragmentToActivity(
                supportFragmentManager, fragment, R.id.content_frame
            )
        }
        // Presenter
        val application = application as LaanoApplication
        application.applicationComponent
            .getManageAccountsComponent(ManageAccountsPresenterModule(fragment))
            .inject(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        setResult(RESULT_OK, resultIntent)
        super.onBackPressed()
    }

    companion object {
        private val TAG = ManageAccountsActivity::class.java.simpleName
    }
}