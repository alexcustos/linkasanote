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

package com.bytesforge.linkasanote.laano.favorites.conflictresolution;

import android.content.Context;

import com.bytesforge.linkasanote.laano.favorites.FavoriteId;

import dagger.Module;
import dagger.Provides;

@Module
public class FavoritesConflictResolutionPresenterModule {

    private final Context context;
    private final FavoritesConflictResolutionContract.View view;
    private String favoriteId;

    public FavoritesConflictResolutionPresenterModule(
            Context context, FavoritesConflictResolutionContract.View view, String favoriteId) {
        this.context = context;
        this.view = view;
        this.favoriteId = favoriteId;
    }

    @Provides
    public FavoritesConflictResolutionContract.View provideFavoritesConflictResolutionContractView() {
        return view;
    }

    @Provides
    public FavoritesConflictResolutionContract.ViewModel provideFavoritesConflictResolutionContractViewModel() {
        return new FavoritesConflictResolutionViewModel(context);
    }

    @Provides
    @FavoriteId
    public String provideFavoriteId() {
        return favoriteId;
    }
}
