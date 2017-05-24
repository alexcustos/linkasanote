package com.bytesforge.linkasanote.manageaccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bytesforge.linkasanote.addeditaccount.AddEditAccountActivity;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public final class ManageAccountsPresenter implements ManageAccountsContract.Presenter {

    private static final String TAG = ManageAccountsPresenter.class.getSimpleName();

    private final ManageAccountsContract.View view;
    private final AccountManager accountManager;
    private final BaseSchedulerProvider schedulerProvider;

    @NonNull
    private final CompositeDisposable disposable;

    @Inject
    public ManageAccountsPresenter(
            ManageAccountsContract.View view, AccountManager accountManager,
            BaseSchedulerProvider schedulerProvider) {
        this.view = view;
        this.accountManager = accountManager;
        this.schedulerProvider = schedulerProvider;
        disposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setAccountManager(accountManager);
    }

    @Override
    public void subscribe() {
        loadAccountItems(true);
    }

    @Override
    public void loadAccountItems(final boolean showLoading) {
        disposable.clear();
        Disposable disposable = view.loadAccountItems()
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        view::swapItems,
                        // NullPointerException
                        throwable -> view.showNotEnoughPermissionsSnackbar());
        this.disposable.add(disposable);
    }

    @Override
    public void unsubscribe() {
        disposable.clear();
    }

    @Override
    public void result(int requestCode, int resultCode) {
        if (AddEditAccountActivity.REQUEST_UPDATE_NEXTCLOUD_ACCOUNT == requestCode
                && Activity.RESULT_OK == resultCode) {
            view.showSuccessfullyUpdatedSnackbar();
        }
    }

    public void onAddClick() {
        view.addAccount();
    }

    public void onEditClick(Account account) {
        view.editAccount(account);
    }

    public void onRemoveClick(Account account) {
        view.confirmAccountRemoval(account);
    }

    public boolean onImageButtonLongClick(ImageButton view) {
        Context context = view.getContext();
        Toast toast = Toast.makeText(context, view.getContentDescription(), Toast.LENGTH_SHORT);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int xOffset = displayMetrics.widthPixels - view.getLeft();
        int yOffset = view.getBottom() + view.getHeight();
        toast.setGravity(Gravity.TOP|Gravity.END, xOffset, yOffset);
        toast.show();
        return true;
    }
}
