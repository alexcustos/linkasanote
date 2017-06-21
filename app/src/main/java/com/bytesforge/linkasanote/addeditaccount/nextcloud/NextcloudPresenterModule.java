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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.content.Context;
import android.support.annotation.Nullable;

import dagger.Module;
import dagger.Provides;

@Module
public class NextcloudPresenterModule {

    private final NextcloudContract.View view;

    @Nullable
    private final Account account;

    @Nullable
    private final AccountAuthenticatorResponse accountAuthenticatorResponse;

    public NextcloudPresenterModule(
            NextcloudContract.View view, @Nullable Account account,
            @Nullable AccountAuthenticatorResponse accountAuthenticatorResponse) {
        this.view = view;
        this.account = account;
        this.accountAuthenticatorResponse = accountAuthenticatorResponse;
    }

    @Provides
    public NextcloudContract.View provideNextcloudContractView() {
        return view;
    }

    @Provides
    public NextcloudContract.ViewModel provideNextcloudContractViewModel(Context context) {
        return new NextcloudViewModel(context);
    }

    @Provides
    @Nullable
    @NextcloudAccount
    public Account provideAccount() {
        return account;
    }

    @Provides
    @Nullable
    public AccountAuthenticatorResponse provideAccountAuthenticatorResponse() {
        return accountAuthenticatorResponse;
    }
}
