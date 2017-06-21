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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;

import java.util.List;

import io.reactivex.Single;

public interface ManageAccountsContract {

    interface View extends BaseView<Presenter> {

        void setAccountManager(@NonNull AccountManager accountManager);
        boolean isActive();
        void finishActivity();
        void cancelActivity();

        void addAccount();
        void editAccount(Account account);
        void confirmAccountRemoval(Account account);

        @Nullable Account[] getAccountsWithPermissionCheck();
        Single<List<AccountItem>> loadAccountItems();
        void swapItems(@NonNull List<AccountItem> accountItems);

        void showSuccessfullyUpdatedSnackbar();
        void showNotEnoughPermissionsSnackbar();
    }

    interface ViewModel extends BaseView<Presenter> {
    }


    interface Presenter extends BasePresenter {

        void result(int requestCode, int resultCode);
        void loadAccountItems(final boolean showLoading);
    }
}
