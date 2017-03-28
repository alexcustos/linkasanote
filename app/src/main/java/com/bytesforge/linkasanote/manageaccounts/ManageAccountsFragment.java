package com.bytesforge.linkasanote.manageaccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.addeditaccount.AddEditAccountActivity;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudFragment;
import com.bytesforge.linkasanote.databinding.FragmentManageAccountsBinding;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.accounts.AccountUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountType;
import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountUsername;
import static com.google.common.base.Preconditions.checkNotNull;

public class ManageAccountsFragment extends Fragment implements ManageAccountsContract.View {

    private static final String TAG = ManageAccountsFragment.class.getSimpleName();
    private static final Handler handler = new Handler();

    private ManageAccountsContract.Presenter presenter;
    private AccountsAdapter adapter;
    private FragmentManageAccountsBinding binding;
    private AccountManager accountManager;

    public static ManageAccountsFragment newInstance() {
        return new ManageAccountsFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.unsubscribe();
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void setPresenter(@NonNull ManageAccountsContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setAccountManager(@NonNull AccountManager accountManager) {
        this.accountManager = checkNotNull(accountManager);
    }

    @Override
    public void finishActivity() {
        getActivity().onBackPressed();
    }

    @Override
    public void cancelActivity() {
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        presenter.result(requestCode, resultCode);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentManageAccountsBinding.inflate(inflater, container, false);
        // RecyclerView
        RecyclerView rvAccounts = binding.rvAccounts;
        if (rvAccounts != null) {
            List<AccountItem> accountItems = new ArrayList<>();
            adapter = new AccountsAdapter((ManageAccountsPresenter) presenter, accountItems);
            rvAccounts.setAdapter(adapter);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            rvAccounts.setLayoutManager(layoutManager);
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                    rvAccounts.getContext(), layoutManager.getOrientation());
            rvAccounts.addItemDecoration(dividerItemDecoration);
        }
        return binding.getRoot();
    }

    @Override
    public Single<List<AccountItem>> loadAccountItems() {
        return Observable.fromCallable(() -> {
            Account[] accounts = getAccountsWithPermissionCheck();
            if (accounts == null) {
                throw new NullPointerException("Required permission was not granted");
            }
            List<AccountItem> accountItems = new LinkedList<>();
            for (Account account : accounts) {
                AccountItem accountItem = new AccountItem(account);
                try {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(account, getContext());
                    accountItem.setDisplayName(ocAccount.getDisplayName());
                } catch (AccountUtils.AccountNotFoundException e) {
                    accountItem.setDisplayName(getAccountUsername(account.name));
                }
                accountItems.add(accountItem);
            }
            if (getResources().getBoolean(R.bool.multiaccount_support) || accounts.length <= 0) {
                accountItems.add(new AccountItem());
            }
            return accountItems;
        }).firstOrError();
    }

    @Override
    public void showNotEnoughPermissionsSnackbar() {
        Snackbar.make(binding.rvAccounts,
                R.string.snackbar_no_permission, Snackbar.LENGTH_LONG)
                .addCallback(new Snackbar.Callback() {

                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        cancelActivity();
                    }
                }).show();
    }

    @Override
    public void showSuccessfullyUpdatedSnackbar() {
        Snackbar.make(binding.rvAccounts,
                R.string.manage_accounts_account_updated, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void addAccount() {
        accountManager.addAccount(getAccountType(getContext()),
                null, null, null, getActivity(), addAccountCallback, handler);
    }

    @Override
    public void editAccount(Account account) {
        Intent updateAccountIntent = new Intent(getContext(), AddEditAccountActivity.class);
        int requestCode = AddEditAccountActivity.REQUEST_UPDATE_NEXTCLOUD_ACCOUNT;

        updateAccountIntent.putExtra(NextcloudFragment.ARGUMENT_EDIT_ACCOUNT_ACCOUNT, account);
        updateAccountIntent.putExtra(AddEditAccountActivity.ARGUMENT_REQUEST_CODE, requestCode);
        startActivityForResult(updateAccountIntent, requestCode);
    }

    @Override
    public void confirmAccountRemoval(Account account) {
        AccountRemovalConfirmationDialog dialog =
                AccountRemovalConfirmationDialog.newInstance(account);
        dialog.setTargetFragment(this, AccountRemovalConfirmationDialog.DIALOG_REQUEST_CODE);
        dialog.show(getFragmentManager(), AccountRemovalConfirmationDialog.DIALOG_TAG);
    }

    public void removeAccount(Account account) {
        accountManager.removeAccount(account, getActivity(), removeAccountCallback, handler);
    }

    public static class AccountRemovalConfirmationDialog extends DialogFragment {

        private static final String ARGUMENT_REMOVAL_CONFIRMATION_ACCOUNT = "ACCOUNT";

        public static final String DIALOG_TAG = "ACCOUNT_REMOVAL_CONFIRMATION";
        public static final int DIALOG_REQUEST_CODE = 0;

        private Account account;

        public static AccountRemovalConfirmationDialog newInstance(@NonNull Account account) {
            checkNotNull(account);

            Bundle args = new Bundle();
            args.putParcelable(ARGUMENT_REMOVAL_CONFIRMATION_ACCOUNT, account);
            AccountRemovalConfirmationDialog dialog = new AccountRemovalConfirmationDialog();
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            account = getArguments().getParcelable(ARGUMENT_REMOVAL_CONFIRMATION_ACCOUNT);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.manage_accounts_removal_confirmation_title)
                    .setMessage(getResources().getString(
                            R.string.manage_accounts_removal_confirmation_message, account.name))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.dialog_button_ok, (dialog, which) ->
                            ((ManageAccountsFragment) getTargetFragment()).removeAccount(account))
                    .setNegativeButton(R.string.dialog_button_cancel, null)
                    .create();
        }
    }

    private AccountManagerCallback<Bundle> removeAccountCallback = future -> {
        if (future != null && future.isDone()) {
            // TODO: disable sync with removed account
            presenter.loadAccountItems(true);
        }
    };

    private AccountManagerCallback<Bundle> addAccountCallback = future -> {
        if (future == null) return;
        try {
            future.getResult(); // NOTE: see exceptions
            presenter.loadAccountItems(true);
        } catch (OperationCanceledException e) {
            Log.d(TAG, "Account creation canceled");
        } catch (IOException | AuthenticatorException e) {
            Log.e(TAG, "Account creation finished with an exception", e);
        }
    };

    @Override
    @Nullable
    public Account[] getAccountsWithPermissionCheck() {
        return CloudUtils.getAccountsWithPermissionCheck(getContext(), accountManager);
    }

    @Override
    public void swapItems(@NonNull List<AccountItem> accountItems) {
        checkNotNull(accountItems);
        adapter.swapItems(accountItems);
    }

    @VisibleForTesting
    ManageAccountsContract.Presenter getPresenter() {
        return presenter;
    }
}
