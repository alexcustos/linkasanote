package com.bytesforge.linkasanote.synclog;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.SyncResult;
import com.bytesforge.linkasanote.databinding.ItemSyncLogBinding;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class SyncLogAdapter extends RecyclerView.Adapter<SyncLogAdapter.ViewHolder> {

    private static final String TAG = SyncLogAdapter.class.getSimpleName();

    private final SyncLogViewModel viewModel;

    private List<SyncResult> syncResults;

    public SyncLogAdapter(
            @NonNull List<SyncResult> syncResults,
            @NonNull SyncLogViewModel viewModel) {
        this.syncResults = checkNotNull(syncResults);
        this.viewModel = checkNotNull(viewModel);
        setHasStableIds(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemSyncLogBinding binding;

        public ViewHolder(ItemSyncLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(
                int position, boolean showStarted,
                SyncResult syncResult, SyncLogViewModel viewModel) {
            binding.setPosition(position);
            binding.setShowStarted(showStarted);
            binding.setSyncResult(syncResult);
            binding.setViewModel(viewModel); // NOTE: global viewModel for fragment and all items

            binding.executePendingBindings();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemSyncLogBinding binding = ItemSyncLogBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SyncResult syncResult = syncResults.get(position);
        boolean showStarted = false;
        if (position <= 0) {
            showStarted = true;
        } else {
            SyncResult prevSyncResult = syncResults.get(position - 1);
            if (syncResult.getStarted() != prevSyncResult.getStarted()) {
                showStarted = true;
            }
        }
        holder.bind(position, showStarted, syncResult, viewModel);
    }

    @Override
    public int getItemCount() {
        return syncResults.size();
    }

    @Override
    public long getItemId(int position) {
        return syncResults.get(position).getRowId();
    }

    // Items

    @NonNull
    public List<SyncResult> getSyncResults() {
        return syncResults;
    }

    public void swapItems(@NonNull List<SyncResult> syncResults) {
        checkNotNull(syncResults);
        this.syncResults = syncResults;
        notifyDataSetChanged();
    }
}
