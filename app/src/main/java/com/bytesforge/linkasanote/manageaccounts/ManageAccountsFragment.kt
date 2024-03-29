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

import android.accounts.*
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.addeditaccount.AddEditAccountActivity
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudFragment
import com.bytesforge.linkasanote.databinding.FragmentManageAccountsBinding
import com.bytesforge.linkasanote.settings.Settings
import com.bytesforge.linkasanote.utils.CloudUtils
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Single
import java.io.IOException
import java.util.*

class ManageAccountsFragment : Fragment(), ManageAccountsContract.View {
    @get:VisibleForTesting
    var presenter: ManageAccountsContract.Presenter? = null
        private set
    private var adapter: AccountsAdapter? = null
    private var binding: FragmentManageAccountsBinding? = null
    private var accountManager: AccountManager? = null

    override fun onResume() {
        super.onResume()
        presenter!!.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter!!.unsubscribe()
    }

    override val isActive: Boolean
        get() = isAdded

    override fun setPresenter(presenter: ManageAccountsContract.Presenter) {
        this.presenter = presenter
    }

    override fun setAccountManager(accountManager: AccountManager) {
        this.accountManager = accountManager
    }

    override fun finishActivity() {
        requireActivity().onBackPressed()
    }

    override fun cancelActivity() {
        requireActivity().setResult(Activity.RESULT_CANCELED)
        requireActivity().finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        presenter!!.result(requestCode, resultCode)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentManageAccountsBinding.inflate(inflater, container, false)
        // RecyclerView
        setupAccountsRecyclerView(binding!!.rvAccounts)
        return binding!!.root
    }

    private fun setupAccountsRecyclerView(rvAccounts: RecyclerView) {
        val accountItems: MutableList<AccountItem> = ArrayList()
        adapter = AccountsAdapter((presenter as ManageAccountsPresenter?)!!, accountItems)
        rvAccounts.adapter = adapter
        val layoutManager = LinearLayoutManager(context)
        rvAccounts.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(
            rvAccounts.context, layoutManager.orientation
        )
        rvAccounts.addItemDecoration(dividerItemDecoration)
    }

    override fun loadAccountItems(): Single<List<AccountItem>> {
        return Single.fromCallable {
            val accounts = accountsWithPermissionCheck
                ?: throw NullPointerException("Required permission was not granted")
            val accountItems: MutableList<AccountItem> = LinkedList()
            for (account in accounts) {
                val accountItem = CloudUtils.getAccountItem(account, requireContext())
                accountItems.add(accountItem)
            }
            if (Settings.GLOBAL_MULTIACCOUNT_SUPPORT || accounts.isEmpty()) {
                accountItems.add(AccountItem())
            }
            accountItems
        }
    }

    override fun showNotEnoughPermissionsSnackbar() {
        Snackbar.make(
            binding!!.rvAccounts,
            R.string.snackbar_no_permission, Snackbar.LENGTH_LONG
        )
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    cancelActivity()
                }
            }).show()
    }

    override fun showSuccessfullyUpdatedSnackbar() {
        Snackbar.make(
            binding!!.rvAccounts,
            R.string.manage_accounts_account_updated, Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun addAccount() {
        accountManager!!.addAccount(
            CloudUtils.getAccountType(requireContext()),
            null,
            null,
            null,
            activity, addAccountCallback, handler
        )
    }

    override fun editAccount(account: Account?) {
        val updateAccountIntent = Intent(context, AddEditAccountActivity::class.java)
        val requestCode = AddEditAccountActivity.REQUEST_UPDATE_NEXTCLOUD_ACCOUNT
        updateAccountIntent.putExtra(NextcloudFragment.ARGUMENT_EDIT_ACCOUNT_ACCOUNT, account)
        updateAccountIntent.putExtra(AddEditAccountActivity.ARGUMENT_REQUEST_CODE, requestCode)
        startActivityForResult(updateAccountIntent, requestCode)
    }

    override fun confirmAccountRemoval(account: Account) {
        val dialog = AccountRemovalConfirmationDialog.newInstance(account)
        dialog.setTargetFragment(this, AccountRemovalConfirmationDialog.DIALOG_REQUEST_CODE)
        val fragmentManager = parentFragmentManager
        dialog.show(
            fragmentManager,
            AccountRemovalConfirmationDialog.DIALOG_TAG
        )
    }

    fun removeAccount(account: Account?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            accountManager!!.removeAccount(account, activity, removeAccountCallback, handler)
        } else {
            accountManager!!.removeAccount(account, removeAccountCallbackCompat, handler)
        }
    }

    class AccountRemovalConfirmationDialog : DialogFragment() {
        private var account: Account? = null
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            account = requireArguments().getParcelable(ARGUMENT_REMOVAL_CONFIRMATION_ACCOUNT)
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(requireContext())
                .setTitle(R.string.manage_accounts_removal_confirmation_title)
                .setMessage(
                    resources.getString(
                        R.string.manage_accounts_removal_confirmation_message, account!!.name
                    )
                )
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton(R.string.dialog_button_delete) { dialog: DialogInterface?, which: Int ->
                    (targetFragment as ManageAccountsFragment?)!!.removeAccount(
                        account
                    )
                }
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .create()
        }

        companion object {
            private const val ARGUMENT_REMOVAL_CONFIRMATION_ACCOUNT = "ACCOUNT"
            const val DIALOG_TAG = "ACCOUNT_REMOVAL_CONFIRMATION"
            const val DIALOG_REQUEST_CODE = 0
            fun newInstance(account: Account): AccountRemovalConfirmationDialog {
                val args = Bundle()
                args.putParcelable(ARGUMENT_REMOVAL_CONFIRMATION_ACCOUNT, account)
                val dialog = AccountRemovalConfirmationDialog()
                dialog.arguments = args
                return dialog
            }
        }
    }

    private val removeAccountCallback =
        AccountManagerCallback { future: AccountManagerFuture<Bundle?> ->
            if (future.isDone) {
                // NOTE: sync successfully completes if account is removed in the middle
                presenter!!.loadAccountItems(true)
            }
        }
    private val removeAccountCallbackCompat =
        AccountManagerCallback { future: AccountManagerFuture<Boolean?> ->
            if (future.isDone) {
                presenter!!.loadAccountItems(true)
            }
        }
    private val addAccountCallback =
        AccountManagerCallback { future: AccountManagerFuture<Bundle?> ->
            try {
                future.result // NOTE: see exceptions
                presenter!!.loadAccountItems(true)
            } catch (e: OperationCanceledException) {
                Log.d(TAG, "Account creation canceled")
            } catch (e: IOException) {
                Log.e(TAG, "Account creation finished with an exception", e)
            } catch (e: AuthenticatorException) {
                Log.e(TAG, "Account creation finished with an exception", e)
            }
        }
    override val accountsWithPermissionCheck: Array<Account>?
        get() = CloudUtils.getAccountsWithPermissionCheck(requireContext(), accountManager!!)

    override fun swapItems(accountItems: List<AccountItem>) {
        adapter!!.swapItems(accountItems)
    }

    companion object {
        private val TAG = ManageAccountsFragment::class.java.simpleName
        private val handler = Handler()
        fun newInstance(): ManageAccountsFragment {
            return ManageAccountsFragment()
        }
    }
}