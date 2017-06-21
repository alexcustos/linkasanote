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

package com.bytesforge.linkasanote.manageaccounts;

import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.databinding.ItemManageAccountsAddBinding;
import com.bytesforge.linkasanote.databinding.ItemManageAccountsBinding;

import java.security.InvalidParameterException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.ViewHolder> {

    private List<AccountItem> accountItems;

    @NonNull
    private ManageAccountsPresenter presenter;

    public AccountsAdapter(
            @NonNull ManageAccountsPresenter presenter, List<AccountItem> accountItems) {
        this.presenter = checkNotNull(presenter);
        this.accountItems = accountItems;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding binding;

        public ViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(AccountItem accountItem) {
            if (accountItem.getType() == AccountItem.TYPE_ACCOUNT) {
                ((ItemManageAccountsBinding) binding).setAccountItem(accountItem);
            }
            binding.executePendingBindings();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == AccountItem.TYPE_ACCOUNT) {
            ItemManageAccountsBinding binding =
                    ItemManageAccountsBinding.inflate(inflater, parent, false);
            binding.setPresenter(presenter);

            return new ViewHolder(binding);
        } else if (viewType == AccountItem.TYPE_ACTION_ADD) {
            ItemManageAccountsAddBinding binding =
                    ItemManageAccountsAddBinding.inflate(inflater, parent, false);
            binding.setPresenter(presenter);

            return new ViewHolder(binding);
        } else {
            throw new InvalidParameterException("Unexpected AccountItem type ID [" + viewType + "]");
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AccountItem accountItem = accountItems.get(position);
        holder.bind(accountItem);
    }

    @Override
    public int getItemCount() {
        return accountItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return accountItems.get(position).getType();
    }

    public void swapItems(@NonNull List<AccountItem> accountItems) {
        checkNotNull(accountItems);

        final AccountItemDiffCallback diffCallback =
                new AccountItemDiffCallback(this.accountItems, accountItems);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.accountItems.clear();
        this.accountItems.addAll(accountItems);

        diffResult.dispatchUpdatesTo(this);
    }

    public class AccountItemDiffCallback extends DiffUtil.Callback {

        private List<AccountItem> oldList;
        private List<AccountItem> newList;

        public AccountItemDiffCallback(List<AccountItem> oldList, List<AccountItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            String oldAccountName = oldList.get(oldItemPosition).getAccountName();
            String newAccountName = newList.get(newItemPosition).getAccountName();

            return (oldAccountName == null && newAccountName == null)
                    || (oldAccountName != null && oldAccountName.equals(newAccountName));

        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            AccountItem oldItem = oldList.get(oldItemPosition);
            AccountItem newItem = newList.get(newItemPosition);
            return oldItem.equals(newItem);
        }
    }
}
