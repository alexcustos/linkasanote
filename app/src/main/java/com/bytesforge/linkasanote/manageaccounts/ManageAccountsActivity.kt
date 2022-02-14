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

import android.accounts.Account
import com.bytesforge.linkasanote.manageaccounts.AccountItem
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsPresenter
import androidx.recyclerview.widget.RecyclerView
import androidx.databinding.ViewDataBinding
import android.view.ViewGroup
import android.view.LayoutInflater
import com.bytesforge.linkasanote.manageaccounts.AccountsAdapter.AccountItemDiffCallback
import androidx.recyclerview.widget.DiffUtil.DiffResult
import androidx.recyclerview.widget.DiffUtil
import androidx.appcompat.app.AppCompatActivity
import javax.inject.Inject
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsFragment
import com.bytesforge.linkasanote.utils.ActivityUtils
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsPresenterModule
import android.content.Intent
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsActivity
import com.bytesforge.linkasanote.BaseView
import android.accounts.AccountManager
import com.bytesforge.linkasanote.BasePresenter
import com.bytesforge.linkasanote.manageaccounts.AccountsAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.bytesforge.linkasanote.utils.CloudUtils
import com.google.android.material.snackbar.Snackbar
import com.bytesforge.linkasanote.addeditaccount.AddEditAccountActivity
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudFragment
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsFragment.AccountRemovalConfirmationDialog
import android.content.DialogInterface
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import com.bytesforge.linkasanote.FragmentScoped
import dagger.Subcomponent
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import android.widget.ImageButton
import android.widget.Toast
import android.util.DisplayMetrics
import android.view.Gravity
import com.bytesforge.linkasanote.databinding.ActivityManageAccountsBinding
import dagger.Provides

class ManageAccountsActivity : AppCompatActivity() {
    @Inject
    var presenter: ManageAccountsPresenter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityManageAccountsBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_manage_accounts)
        // Toolbar
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.actionbar_title_manage_accounts)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
        }
        // Fragment
        var fragment = supportFragmentManager
            .findFragmentById(R.id.content_frame) as ManageAccountsFragment?
        if (fragment == null) {
            fragment = ManageAccountsFragment.Companion.newInstance()
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