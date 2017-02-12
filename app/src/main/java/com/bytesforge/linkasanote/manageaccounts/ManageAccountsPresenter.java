package com.bytesforge.linkasanote.manageaccounts;

import android.accounts.Account;
import android.app.Activity;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.addeditaccount.AddEditAccountActivity;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.List;

import javax.inject.Inject;

import rx.Observer;
import rx.subscriptions.CompositeSubscription;

public final class ManageAccountsPresenter implements ManageAccountsContract.Presenter {

    private static final String TAG = ManageAccountsPresenter.class.getSimpleName();

    private final ManageAccountsContract.View view;
    private final BaseSchedulerProvider schedulerProvider;

    @NonNull
    private final CompositeSubscription subscription;

    @Inject
    public ManageAccountsPresenter(
            ManageAccountsContract.View view,
            BaseSchedulerProvider schedulerProvider) {
        this.view = view;
        this.schedulerProvider = schedulerProvider;

        subscription = new CompositeSubscription();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
    }

    @Override
    public void subscribe() {
        loadAccountItems(true);
    }

    @Override
    public void loadAccountItems(final boolean showLoading) {
        // TODO: implement SwipeRefreshLayout
        EspressoIdlingResource.increment();
        subscription.clear();

        view.loadAccountItems()
                .toList()
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .doOnTerminate(() -> {
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
                })
                .subscribe(new Observer<List<AccountItem>>() {

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.showNotEnoughPermissionsSnackbar();
                    }

                    @Override
                    public void onNext(List<AccountItem> accountItems) {
                        view.swapItems(accountItems);
                    }
                });
        this.subscription.add(subscription);
    }

    @Override
    public void unsubscribe() {
        subscription.clear();
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
