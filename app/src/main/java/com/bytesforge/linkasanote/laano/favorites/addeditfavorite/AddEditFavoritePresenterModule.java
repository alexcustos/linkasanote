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

package com.bytesforge.linkasanote.laano.favorites.addeditfavorite;

import android.content.Context;

import androidx.annotation.Nullable;

import com.bytesforge.linkasanote.laano.favorites.FavoriteId;

import dagger.Module;
import dagger.Provides;

@Module
public class AddEditFavoritePresenterModule {

    private final Context context;
    private final AddEditFavoriteContract.View view;
    private String favoriteId;

    public AddEditFavoritePresenterModule(
            Context context, AddEditFavoriteContract.View view, @Nullable String favoriteId) {
        this.context = context;
        this.view = view;
        this.favoriteId = favoriteId;
    }

    @Provides
    public AddEditFavoriteContract.View provideAddEditFavoriteContractView() {
        return view;
    }

    @Provides
    public AddEditFavoriteContract.ViewModel provideAddEditFavoriteContractViewModel() {
        return new AddEditFavoriteViewModel(context);
    }

    @Provides
    @Nullable
    @FavoriteId
    public String provideFavoriteId() {
        return favoriteId;
    }
}
