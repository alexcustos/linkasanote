package com.bytesforge.linkasanote.manageaccounts;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.test.espresso.Espresso;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountType;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@LargeTest
@SuppressWarnings("MissingPermission")
public class ManageAccountsActivityTest {

    private static final String ACCOUNT_TYPE = getAccountType();
    private static final String USER_NAME = "demo";
    private static final Account[] ACCOUNTS = {
            new Account(USER_NAME + "@demo.nextcloud.com", ACCOUNT_TYPE)};

    @Rule
    public ActivityTestRule<ManageAccountsActivity> manageAccountsActivityTestRule =
            new ActivityTestRule<>(ManageAccountsActivity.class);

    private AccountManager mockAccountManager;
    private ManageAccountsContract.Presenter presenter;

    @Before
    public void setupManageAccountsActivity() {
        TestUtils.allowPermissionIfNeeded(Manifest.permission.GET_ACCOUNTS);
        mockAccountManager = Mockito.mock(AccountManager.class);

        ManageAccountsActivity activity = manageAccountsActivityTestRule.getActivity();
        assertThat(activity, notNullValue());
        ManageAccountsFragment fragment = (ManageAccountsFragment) activity
                .getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        assertThat(fragment, notNullValue());
        fragment.setAccountManager(mockAccountManager);
        presenter = fragment.getPresenter();
        assertThat(presenter, notNullValue());
    }

    @Before
    public void registerIdlingResource() {
        Espresso.registerIdlingResources(
                manageAccountsActivityTestRule.getActivity().getCountingIdlingResource());
    }

    @After
    public void unregisterIdlingResource() {
        Espresso.unregisterIdlingResources(
                manageAccountsActivityTestRule.getActivity().getCountingIdlingResource());
    }

    @Test
    public void checkInitialState() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(new Account[0]);
        // TODO: look for better way then reloading with mockAccountManager
        presenter.loadAccountItems(true);
        onView(withId(R.id.add_account_view)).check(
                matches(withText(R.string.item_manage_accounts_add)));
    }

    @Test
    public void checkInitialStateWithOneAccountAdded() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(ACCOUNTS);
        presenter.loadAccountItems(true);
        onView(withId(R.id.add_account_view)).check(
                matches(withText(R.string.item_manage_accounts_add)));
        onView(withId(R.id.user_name)).check(matches(withText(USER_NAME)));
        onView(withId(R.id.account_name)).check(matches(withText(ACCOUNTS[0].name)));
    }

    @Test
    public void clickOnAddItem_MakesAccountManagerAddAccountCall() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(ACCOUNTS);
        presenter.loadAccountItems(true);
        onView(withId(R.id.add_account_view)).perform(click());
        verify(mockAccountManager).addAccount(eq(getAccountType()), isNull(String.class),
                isNull(String[].class), isNull(Bundle.class), any(Activity.class),
                Matchers.<AccountManagerCallback<Bundle>>any(), any(Handler.class));
    }

    @Test
    public void clickOnItemEditButton_OpensAddEditAccountActivityWithAppropriateUsername() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(ACCOUNTS);
        presenter.loadAccountItems(true);
        TestUtils.sleep(250); // TODO: gracefully wait for UI
        onView(withId(R.id.account_edit_button)).perform(click());
        onView(withId(R.id.account_username)).check(matches(withText(USER_NAME)));
    }

    @Test
    public void clickOnItemDeleteButton_AfterConfirmationMakesAccountManagerRemoveAccountCall() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(ACCOUNTS);
        presenter.loadAccountItems(true);
        TestUtils.sleep(250); // TODO: gracefully wait for UI
        onView(withId(R.id.account_delete_button)).perform(click());
        onView(withText(R.string.dialog_button_ok)).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText(R.string.dialog_button_ok)).inRoot(isDialog()).perform(click());
        verify(mockAccountManager).removeAccount(any(Account.class), any(Activity.class),
                Matchers.<AccountManagerCallback<Bundle>>any(), any(Handler.class));
    }
}