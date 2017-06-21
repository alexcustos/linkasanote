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

package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;

import com.bytesforge.linkasanote.databinding.DrawerHeaderBinding;

import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoViewModel extends BaseObservable {

    @Bindable
    public LaanoDrawerHeaderViewModel headerViewModel;

    public LaanoViewModel(@NonNull Context context) {
        headerViewModel = new LaanoDrawerHeaderViewModel(checkNotNull(context));
    }

    @BindingAdapter({"headerViewModel"})
    public static void setupDrawerHeader(
            NavigationView view, LaanoDrawerHeaderViewModel viewModel) {
        DrawerHeaderBinding binding = DrawerHeaderBinding.inflate(
                LayoutInflater.from(view.getContext()), view, false);
        binding.setViewModel(viewModel);
        binding.executePendingBindings();
        view.addHeaderView(binding.getRoot());
    }

    public void setInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            applyInstanceState(getDefaultInstanceState());
        } else {
            applyInstanceState(savedInstanceState);
        }
        headerViewModel.setInstanceState(savedInstanceState);
    }

    public void saveInstanceState(@NonNull Bundle outState) {
        checkNotNull(outState);
        headerViewModel.saveInstanceState(outState);
    }

    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        notifyChange();
    }

    public Bundle getDefaultInstanceState() {
        return new Bundle();
    }
}
