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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.ApplicationComponent;
import com.bytesforge.linkasanote.ApplicationModule;
import com.bytesforge.linkasanote.DaggerApplicationComponent;
import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.settings.SettingsModule;
import com.bytesforge.linkasanote.utils.schedulers.SchedulerProviderModule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

    private static final String USERNAME = "demo";

    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private LaanoApplication laanoApplication = (LaanoApplication) context.getApplicationContext();

    /* EXAMPLE: how to inject mock to the module
    @Rule
    public DaggerMockRule<ApplicationComponent> daggerMockRule = new DaggerMockRule<>(
            ApplicationComponent.class, new ApplicationModule(context)).set(component ->
            ((LaanoApplication) context.getApplicationContext()).setApplicationComponent(component));*/

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.GET_ACCOUNTS);

    @Rule
    public ActivityTestRule<ManageAccountsActivity> manageAccountsActivityTestRule =
            new ActivityTestRule<ManageAccountsActivity>(ManageAccountsActivity.class, false, false) {
                private ApplicationComponent applicationComponent;

                @Override
                protected void beforeActivityLaunched() {
                    super.beforeActivityLaunched();
                    applicationComponent = setupMockApplicationComponent(mockAccountManager);
                }

                @Override
                protected void afterActivityLaunched() {
                    super.afterActivityLaunched();
                    setupManageAccountsActivity();
                }
            };

    private String ACCOUNT_TYPE;
    private Account[] ACCOUNTS;

    @Mock
    AccountManager mockAccountManager;

    public ManageAccountsActivityTest() {
        MockitoAnnotations.initMocks(this);
        ACCOUNT_TYPE = getAccountType(context);
        ACCOUNTS = new Account[]{new Account(USERNAME + "@demo.nextcloud.com", ACCOUNT_TYPE)};
    }

    private ApplicationComponent setupMockApplicationComponent(AccountManager accountManager) {
        ApplicationComponent oldApplicationComponent = laanoApplication.getApplicationComponent();
        ApplicationComponent applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(laanoApplication) {

                    @Override
                    public AccountManager provideAccountManager() {
                        return accountManager;
                    }
                })
                .settingsModule(new SettingsModule())
                .repositoryModule(new RepositoryModule())
                .providerModule(new ProviderModule())
                .schedulerProviderModule(new SchedulerProviderModule())
                .build();
        laanoApplication.setApplicationComponent(applicationComponent);
        return oldApplicationComponent;
    }

    private void setupManageAccountsActivity() { // @Before
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

        if (Settings.GLOBAL_MULTIACCOUNT_SUPPORT) {
            onView(withId(R.id.add_account_view)).check(
                    matches(withText(R.string.item_manage_accounts_add)));
        }
        onView(withId(R.id.user_name)).check(matches(withText(USERNAME)));
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
        onView(withId(R.id.account_username)).check(matches(withText(USERNAME)));
    }

    @Test
    public void clickOnItemDeleteButton_AfterConfirmationMakesAccountManagerRemoveAccountCall() {
        when(mockAccountManager.getAccountsByType(anyString())).thenReturn(ACCOUNTS);
        manageAccountsActivityTestRule.launchActivity(null);

        onView(withId(R.id.account_delete_button)).perform(click());
        onView(withText(R.string.dialog_button_delete)).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText(R.string.dialog_button_delete)).inRoot(isDialog()).perform(click());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            verify(mockAccountManager).removeAccount(any(Account.class), any(Activity.class),
                    ArgumentMatchers.<AccountManagerCallback<Bundle>>any(), any(Handler.class));
        } else {
            verify(mockAccountManager).removeAccount(any(Account.class),
                    ArgumentMatchers.<AccountManagerCallback<Boolean>>any(), any(Handler.class));
        }
    }
}