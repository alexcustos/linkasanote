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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.databinding.DialogFavoriteConflictResolutionBinding;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesConflictResolutionDialog extends DialogFragment implements
        FavoritesConflictResolutionContract.View {

    public static final String ARGUMENT_FAVORITE_ID = "FAVORITE_ID";

    public static final String DIALOG_TAG = "CONFLICT_RESOLUTION";
    public static final int RESULT_OK = Activity.RESULT_OK;
    public static final int RESULT_FAILED = Activity.RESULT_FIRST_USER;

    @Inject
    FavoritesConflictResolutionPresenter presenter;

    FavoritesConflictResolutionContract.ViewModel viewModel;

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.unsubscribe();
    }

    @Override
    public void setPresenter(@NonNull FavoritesConflictResolutionContract.Presenter presenter) {
        // NOTE: presenter is injected directly to this dialogFragment
    }

    @Override
    public void setViewModel(@NonNull FavoritesConflictResolutionContract.ViewModel viewModel) {
        this.viewModel = checkNotNull(viewModel);
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void finishActivity() {
        dismiss();
        getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, null);
    }

    @Override
    public void cancelActivity() {
        dismiss();
        getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_FAILED, null);
    }

    public static FavoritesConflictResolutionDialog newInstance(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        Bundle args = new Bundle();
        args.putString(ARGUMENT_FAVORITE_ID, favoriteId);
        FavoritesConflictResolutionDialog dialog = new FavoritesConflictResolutionDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String favoriteId = getArguments().getString(ARGUMENT_FAVORITE_ID);
        // Presenter
        LaanoApplication application = (LaanoApplication) getActivity().getApplication();
        application.getApplicationComponent()
                .getFavoritesConflictResolutionComponent(
                        new FavoritesConflictResolutionPresenterModule(getContext(), this, favoriteId))
                .inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        LayoutInflater inflater = LayoutInflater.from(context);
        DialogFavoriteConflictResolutionBinding binding =
                DialogFavoriteConflictResolutionBinding.inflate(inflater, null, false);
        viewModel.setInstanceState(savedInstanceState);
        binding.setPresenter(presenter);
        binding.setViewModel((FavoritesConflictResolutionViewModel) viewModel);
        return new AlertDialog.Builder(getContext())
                .setView(binding.getRoot())
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.saveInstanceState(outState);
    }
}
