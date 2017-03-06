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
