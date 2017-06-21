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

package com.bytesforge.linkasanote.sync.operations;

import android.content.Intent;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static com.bytesforge.linkasanote.utils.CommonUtils.convertIdn;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.any;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OperationsServiceTest {

    private final String SERVER_URL = "https://demo.nextcloud.com";

    @Rule
    public final ServiceTestRule operationsServiceRule = new ServiceTestRule();

    @Test
    public void queueOperation_queuesOneAndReturnsHashCode() throws TimeoutException {
        Intent serviceIntent = new Intent(
                InstrumentationRegistry.getTargetContext(), OperationsService.class);
        IBinder binder = operationsServiceRule.bindService(serviceIntent);
        OperationsService service = ((OperationsService.OperationsBinder) binder).getService();

        Intent getServerInfoIntent = new Intent();
        getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO);
        getServerInfoIntent.putExtra(
                OperationsService.EXTRA_SERVER_URL, convertIdn(SERVER_URL, true));

        assertThat(service.queueOperation(getServerInfoIntent, null, null), is(any(long.class)));
        assertThat(service.getPendingOperationsQueueSize(), equalTo(1));
    }
}
