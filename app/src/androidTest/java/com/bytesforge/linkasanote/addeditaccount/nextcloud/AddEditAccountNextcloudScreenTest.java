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

package com.bytesforge.linkasanote.addeditaccount.nextcloud;

import android.Manifest;
import android.content.Intent;
import android.os.Handler;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.addeditaccount.AddEditAccountActivity;
import com.bytesforge.linkasanote.sync.operations.OperationsService;
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.bytesforge.linkasanote.EspressoMatchers.withDrawable;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddEditAccountNextcloudScreenTest {

    private final String MALFORMED_URL = "demo.nextcloud.com:port";
    private final String UNFORMATTED_URL = "Demo.Nextcloud.com:443/index.php/apps/files/";
    private final String SERVER_URL = "https://demo.nextcloud.com";
    private final String USERNAME = "demo";
    private final String PASSWORD = "password";
    private final String SERVER_VERSION = "0.0.0.0";

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.GET_ACCOUNTS);

    @Rule
    public ActivityTestRule<AddEditAccountActivity> addEditAccountActivityTestRule =
            new ActivityTestRule<>(AddEditAccountActivity.class);

    @Mock
    private OperationsService serviceMock;

    private ArgumentCaptor<NextcloudFragment> viewCaptor;

    @Before
    public void setupAddEditAccountActivityNextcloud() {
        MockitoAnnotations.initMocks(this);

        AddEditAccountActivity activity = addEditAccountActivityTestRule.getActivity();
        assertThat(activity, notNullValue());

        NextcloudFragment fragment = (NextcloudFragment) activity
                .getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        assertThat(fragment, notNullValue());

        NextcloudContract.Presenter presenter = fragment.getPresenter();
        assertThat(presenter, notNullValue());

        fragment.setOperationsService(serviceMock);
        viewCaptor = ArgumentCaptor.forClass(NextcloudFragment.class);
    }

    @Test
    public void checkInitialState() {
        onView(withId(R.id.server_url)).perform(clearText());
        onView(withId(R.id.account_username)).perform(clearText());
        onView(withId(R.id.account_password)).perform(clearText());

        onView(withId(R.id.server_status)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.server_status)).check(matches(withDrawable(R.drawable.ic_warning)));
        onView(withId(R.id.host_url_refresh_button)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.auth_status)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.login_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void lostFocusOnEmptyUrlField_showsEmptyUrlWarning() {
        onView(withId(R.id.server_url)).perform(click(), clearText());
        onView(withId(R.id.server_status)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        onView(withId(R.id.account_username)).perform(click());
        onView(withId(R.id.server_status)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.server_status)).check(matches(withText(
                R.string.add_edit_account_nextcloud_warning_empty_url)));
        onView(withId(R.id.server_status)).check(matches(withDrawable(R.drawable.ic_warning)));
    }

    @Test
    public void lostFocusOnMalformedUrlField_showsMalformedUrlWarning() {
        onView(withId(R.id.server_url)).perform(typeText(MALFORMED_URL));
        onView(withId(R.id.account_username)).perform(click());

        onView(withId(R.id.server_status)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.server_status)).check(matches(withText(
                R.string.add_edit_account_nextcloud_warning_malformed_url)));
        onView(withId(R.id.server_status)).check(matches(withDrawable(R.drawable.ic_warning)));
    }

    @Test
    public void lostFocusOnUnformattedUrlFiled_makesUrlNormalized() {
        onView(withId(R.id.server_url)).perform(typeText(UNFORMATTED_URL));
        onView(withId(R.id.account_username)).perform(click());

        onView(withId(R.id.server_url)).check(matches(withText(SERVER_URL)));
    }

    @Test
    public void lostFocusOnWrongServerUrlField_showsRefreshButton() {
        onView(withId(R.id.server_url)).perform(typeText(SERVER_URL));
        onView(withId(R.id.account_username)).perform(click());

        verify(serviceMock).queueOperation(
                any(Intent.class), viewCaptor.capture(), any(Handler.class));
        viewCaptor.getValue().onRemoteOperationFinish(
                new GetServerInfoOperation(SERVER_URL, serviceMock),
                new RemoteOperationResult(RemoteOperationResult.ResultCode.FILE_NOT_FOUND));
        // Just to make sure UI is updated
        onView(withId(R.id.server_url)).perform(click());
        onView(withId(R.id.host_url_refresh_button)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    @Test
    public void allFieldsAreFilledAndServerUrlIsValid_enablesLoginButton() {
        // Mock result
        RemoteOperationResult result =
                new RemoteOperationResult(RemoteOperationResult.ResultCode.OK_SSL);
        GetServerInfoOperation.ServerInfo serverInfo = new GetServerInfoOperation.ServerInfo();
        serverInfo.version = new OwnCloudVersion(SERVER_VERSION);
        serverInfo.isSecure = true;
        serverInfo.baseUrl = SERVER_URL;
        ArrayList<Object> data = new ArrayList<>();
        data.add(serverInfo);
        result.setData(data);
        // Init
        onView(withId(R.id.login_button)).check(matches(not(isEnabled())));
        // Server URL
        onView(withId(R.id.server_url)).perform(typeText(SERVER_URL));
        onView(withId(R.id.account_username)).perform(click());
        verify(serviceMock).queueOperation(
                any(Intent.class), viewCaptor.capture(), any(Handler.class));
        // Mock OK_SSL Status
        viewCaptor.getValue().onRemoteOperationFinish(
                new GetServerInfoOperation(SERVER_URL, serviceMock), result);
        onView(withId(R.id.server_url)).perform(click());
        // Auth
        onView(withId(R.id.login_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.account_username)).perform(scrollTo(), typeText(USERNAME));
        onView(withId(R.id.login_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.account_password)).perform(scrollTo(), typeText(PASSWORD));
        onView(withId(R.id.login_button)).check(matches(isEnabled()));
        //AndroidTestUtils.rotateOrientation(addEditAccountActivityTestRule);
        // Check
        onView(withId(R.id.server_url)).check(matches(withText(SERVER_URL)));
        onView(withId(R.id.host_url_refresh_button)).check(matches(not(isDisplayed())));
        onView(withId(R.id.server_status)).check(matches(withText(R.string.add_edit_account_nextcloud_connection_secure)));
        onView(withId(R.id.account_username)).check(matches(withText(USERNAME)));
        onView(withId(R.id.account_password)).check(matches(withText(PASSWORD)));
        onView(withId(R.id.auth_status)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.login_button)).check(matches(isEnabled()));
    }
}
