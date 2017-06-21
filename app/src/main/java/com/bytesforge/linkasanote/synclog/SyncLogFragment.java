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

package com.bytesforge.linkasanote.synclog;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.SyncResult;
import com.bytesforge.linkasanote.databinding.FragmentSyncLogBinding;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncLogFragment extends Fragment implements SyncLogContract.View {

    private static final String TAG = SyncLogFragment.class.getSimpleName();

    private SyncLogContract.Presenter presenter;
    private SyncLogContract.ViewModel viewModel;
    SyncLogAdapter adapter;
    LinearLayoutManager rvLayoutManager;
    private Parcelable rvLayoutState;

    public static SyncLogFragment newInstance() {
        return new SyncLogFragment();
    }

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
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void setPresenter(@NonNull SyncLogContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setViewModel(@NonNull SyncLogContract.ViewModel viewModel) {
        this.viewModel = checkNotNull(viewModel);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentSyncLogBinding binding = FragmentSyncLogBinding.inflate(inflater, container, false);
        viewModel.setInstanceState(savedInstanceState);
        setRvLayoutState(savedInstanceState);
        binding.setViewModel((SyncLogViewModel) viewModel);
        setupSyncLogRecyclerView(binding.rvSyncLog);
        return binding.getRoot();
    }

    private void setupSyncLogRecyclerView(RecyclerView rvSyncLog) {
        List<SyncResult> syncResults = new ArrayList<>();
        adapter = new SyncLogAdapter(syncResults, (SyncLogViewModel) viewModel);
        rvSyncLog.setAdapter(adapter);
        rvLayoutManager = new LinearLayoutManager(getContext());
        rvSyncLog.setLayoutManager(rvLayoutManager);
    }

    private void setRvLayoutState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            rvLayoutState = savedInstanceState.getParcelable(
                    SyncLogViewModel.STATE_RECYCLER_LAYOUT);
        }
    }

    private void saveRvLayoutState(@NonNull Bundle outState) {
        checkNotNull(outState);
        outState.putParcelable(SyncLogViewModel.STATE_RECYCLER_LAYOUT,
                rvLayoutManager.onSaveInstanceState());
    }

    private void applyRvLayoutState() {
        if (rvLayoutManager != null && rvLayoutState != null) {
            rvLayoutManager.onRestoreInstanceState(rvLayoutState);
            rvLayoutState = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.saveInstanceState(outState);
        saveRvLayoutState(outState);
    }

    @Override
    public void showSyncResults(@NonNull List<SyncResult> syncResults) {
        checkNotNull(syncResults);
        adapter.swapItems(syncResults);

        viewModel.setListSize(syncResults.size());
        applyRvLayoutState();
    }
}
