package com.bytesforge.linkasanote.manageaccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.addeditaccount.AddEditAccountActivity;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
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
        // TODO: implement showLoading
        EspressoIdlingResource.increment();
        disposable.clear();

        Disposable disposable = view.loadAccountItems()
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doFinally(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                // NullPointerException
                .doOnError(throwable -> view.showNotEnoughPermissionsSnackbar())
                .subscribe((accountItems, throwable) -> {
                    if (accountItems != null) view.swapItems(accountItems);
                });
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
}
