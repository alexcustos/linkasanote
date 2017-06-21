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

package com.bytesforge.linkasanote.about;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class AboutPresenterModule {

    private final Context context;
    private AboutContract.View view;

    public AboutPresenterModule(Context context, AboutContract.View view) {
        this.context = context;
        this.view = view;
    }

    @Provides
    public AboutContract.View provideAboutContractView() {
        return view;
    }

    @Provides
    public AboutContract.ViewModel provideAboutContractViewModel() {
        return new AboutViewModel(context);
    }
}
