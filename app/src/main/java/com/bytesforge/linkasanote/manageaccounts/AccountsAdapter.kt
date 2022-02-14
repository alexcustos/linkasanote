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
import com.bytesforge.linkasanote.databinding.ItemManageAccountsAddBinding
import com.bytesforge.linkasanote.databinding.ItemManageAccountsBinding
import com.google.common.base.Preconditions
import dagger.Provides
import java.security.InvalidParameterException

class AccountsAdapter(
    presenter: ManageAccountsPresenter, accountItems: MutableList<AccountItem?>
) : RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {
    private val accountItems: MutableList<AccountItem?>
    private val presenter: ManageAccountsPresenter

    class ViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(accountItem: AccountItem?) {
            if (accountItem.getType() == AccountItem.Companion.TYPE_ACCOUNT) {
                (binding as ItemManageAccountsBinding).accountItem =
                    accountItem
            }
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == AccountItem.Companion.TYPE_ACCOUNT) {
            val binding = ItemManageAccountsBinding.inflate(
                inflater,
                parent,
                false
            )
            binding.presenter = presenter
            ViewHolder(binding)
        } else if (viewType == AccountItem.Companion.TYPE_ACTION_ADD) {
            val binding = ItemManageAccountsAddBinding.inflate(
                inflater,
                parent,
                false
            )
            binding.presenter = presenter
            ViewHolder(binding)
        } else {
            throw InvalidParameterException("Unexpected AccountItem type ID [$viewType]")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val accountItem = accountItems[position]
        holder.bind(accountItem)
    }

    override fun getItemCount(): Int {
        return accountItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return accountItems[position].getType()
    }

    fun swapItems(accountItems: List<AccountItem?>) {
        Preconditions.checkNotNull(accountItems)
        val diffCallback = AccountItemDiffCallback(this.accountItems, accountItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.accountItems.clear()
        this.accountItems.addAll(accountItems)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class AccountItemDiffCallback(
        private val oldList: List<AccountItem?>,
        private val newList: List<AccountItem?>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldAccountName = oldList[oldItemPosition].getAccountName()
            val newAccountName = newList[newItemPosition].getAccountName()
            return (oldAccountName == null && newAccountName == null
                    || oldAccountName != null && oldAccountName == newAccountName)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }

    init {
        this.presenter = Preconditions.checkNotNull(presenter)
        this.accountItems = accountItems
    }
}