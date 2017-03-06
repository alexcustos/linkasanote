package com.bytesforge.linkasanote.manageaccounts;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.ApplicationComponent;
import com.bytesforge.linkasanote.ApplicationModule;
import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.TestUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import it.cosenonjaviste.daggermock.DaggerMockRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.bytesforge.linkasanote.utils.CloudUtils.getAccountType;
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

    private static final String USER_NAME = "demo";

    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Rule
    public DaggerMockRule<ApplicationComponent> daggerMockRule = new DaggerMockRule<>(
            ApplicationComponent.class, new ApplicationModule(context)).set(component ->
            ((LaanoApplication) context.getApplicationContext()).setApplicationComponent(component));

    @Rule
    public ActivityTestRule<ManageAccountsActivity> manageAccountsActivityTestRule =
            new ActivityTestRule<ManageAccountsActivity>(ManageAccountsActivity.class, false, false) {

                @Override
                protected void afterActivityLaunched() {
                    super.afterActivityLaunched();
                    setupManageAccountsActivity();
                    registerIdlingResource();
                }

                @Override
                protected void afterActivityFinished() {
                    super.afterActivityFinished();
                    unregisterIdlingResource();
                }
            };

    private static String ACCOUNT_TYPE;
    private static Account[] ACCOUNTS;

    @Mock
    AccountManager mockAccountManager;

    public ManageAccountsActivityTest() {
        ACCOUNT_TYPE = getAccountType(context);
        ACCOUNTS = new Account[]{new Account(USER_NAME + "@demo.nextcloud.com", ACCOUNT_TYPE)};
    }

    private void setupManageAccountsActivity() { // @Before
        TestUtils.allowPermissionIfNeeded(Manifest.permission.GET_ACCOUNTS);
    }

    private void registerIdlingResource() { // @Before
        Espresso.registerIdlingResources(
                manageAccountsActivityTestRule.getActivity().getCountingIdlingResource());
    }

    private void unregisterIdlingResource() { // @After
        Espresso.unregisterIdlingResources(
                manageAccountsActivityTestRule.getActivity().getCountingIdlingResource());
    }

    @Test
    public void checkInitialState() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(new Account[0]);
        manageAccountsActivityTestRule.launchActivity(null);

        onView(withId(R.id.add_account_view)).check(
                matches(withText(R.string.item_manage_accounts_add)));
    }

    @Test
    public void checkInitialStateWithOneAccountAdded() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(ACCOUNTS);
        manageAccountsActivityTestRule.launchActivity(null);

        if (context.getResources().getBoolean(R.bool.multiaccount_support)) {
            onView(withId(R.id.add_account_view)).check(
                    matches(withText(R.string.item_manage_accounts_add)));
        }
        onView(withId(R.id.user_name)).check(matches(withText(USER_NAME)));
        onView(withId(R.id.account_name)).check(matches(withText(ACCOUNTS[0].name)));
    }

    @Test
    public void clickOnAddItem_MakesAccountManagerAddAccountCall() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(new Account[0]);
        manageAccountsActivityTestRule.launchActivity(null);

        onView(withId(R.id.add_account_view)).perform(click());
        verify(mockAccountManager).addAccount(eq(ACCOUNT_TYPE), isNull(),
                isNull(), isNull(), any(Activity.class),
                ArgumentMatchers.<AccountManagerCallback<Bundle>>any(), any(Handler.class));
    }

    @Test
    public void clickOnItemEditButton_OpensAddEditAccountActivityWithAppropriateUsername() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(ACCOUNTS);
        manageAccountsActivityTestRule.launchActivity(null);

        onView(withId(R.id.account_edit_button)).perform(click());
        onView(withId(R.id.account_username)).check(matches(withText(USER_NAME)));
    }

    @Test
    public void clickOnItemDeleteButton_AfterConfirmationMakesAccountManagerRemoveAccountCall() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(ACCOUNTS);
        manageAccountsActivityTestRule.launchActivity(null);

        onView(withId(R.id.account_delete_button)).perform(click());
        onView(withText(R.string.dialog_button_ok)).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText(R.string.dialog_button_ok)).inRoot(isDialog()).perform(click());
        verify(mockAccountManager).removeAccount(any(Account.class), any(Activity.class),
                ArgumentMatchers.<AccountManagerCallback<Bundle>>any(), any(Handler.class));
    }
}