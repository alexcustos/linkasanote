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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bytesforge.linkasanote.databinding.ItemManageAccountsAddBinding
import com.bytesforge.linkasanote.databinding.ItemManageAccountsBinding
import java.security.InvalidParameterException

class AccountsAdapter(
    private val presenter: ManageAccountsPresenter,
    private val accountItems: MutableList<AccountItem>
) : RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {

    class ViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(accountItem: AccountItem) {
            if (accountItem.type == AccountItem.TYPE_ACCOUNT) {
                (binding as ItemManageAccountsBinding).accountItem = accountItem
            }
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            AccountItem.TYPE_ACCOUNT -> {
                val binding = ItemManageAccountsBinding.inflate(
                    inflater,
                    parent,
                    false
                )
                binding.presenter = presenter
                ViewHolder(binding)
            }
            AccountItem.TYPE_ACTION_ADD -> {
                val binding = ItemManageAccountsAddBinding.inflate(
                    inflater,
                    parent,
                    false
                )
                binding.presenter = presenter
                ViewHolder(binding)
            }
            else -> {
                throw InvalidParameterException("Unexpected AccountItem type ID [$viewType]")
            }
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
        return accountItems[position].type
    }

    fun swapItems(accountItems: List<AccountItem>) {
        val diffCallback = AccountItemDiffCallback(this.accountItems, accountItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.accountItems.clear()
        this.accountItems.addAll(accountItems)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class AccountItemDiffCallback(
        private val oldList: List<AccountItem>,
        private val newList: List<AccountItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldAccountName = oldList[oldItemPosition].accountName
            val newAccountName = newList[newItemPosition].accountName
            return (oldAccountName == null && newAccountName == null
                    || oldAccountName != null && oldAccountName == newAccountName)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }
}