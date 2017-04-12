package com.bytesforge.linkasanote.laano.links;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.databinding.ItemLinksBinding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class LinksAdapter extends RecyclerView.Adapter<LinksAdapter.ViewHolder> {

    private static final String TAG = LinksAdapter.class.getSimpleName();

    private final LinksContract.Presenter presenter;
    private final LinksViewModel viewModel;

    private List<Link> links;
    private Map<String, Integer> positionMap;

    public LinksAdapter(
            @NonNull List<Link> links,
            @NonNull LinksContract.Presenter presenter,
            @NonNull LinksViewModel viewModel) {
        this.links = checkNotNull(links);
        this.presenter = checkNotNull(presenter);
        this.viewModel = checkNotNull(viewModel);
        setHasStableIds(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLinksBinding binding;

        public ViewHolder(ItemLinksBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(
                Link link, LinksContract.Presenter presenter,
                LinksViewModel viewModel) {
            binding.setLink(link);
            binding.setPresenter(presenter);
            binding.setViewModel(viewModel); // NOTE: global viewModel for fragment and all items

            binding.executePendingBindings();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemLinksBinding binding = ItemLinksBinding.inflate(inflater, parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Link link = links.get(position);
        holder.bind(link, presenter, viewModel);
    }

    @Override
    public int getItemCount() {
        return links.size();
    }

    @Override
    public long getItemId(int position) {
        return links.get(position).getRowId();
    }

    // Items

    @NonNull
    public Link removeItem(int position) {
        Link link = links.remove(position);
        updatePositionMap();
        notifyItemRemoved(position);
        return link;
    }

    public void swapItems(@NonNull List<Link> links) {
        checkNotNull(links);

        this.links = links;
        updatePositionMap();
        notifyDataSetChanged();
    }

    public int getPosition(@Nullable String linkId) {
        if (linkId == null) return 0;

        Integer position = positionMap.get(linkId);
        if (position == null) {
            Log.e(TAG, "No position is found for Link [" + linkId + "]");
            return 0;
        }
        return position;
    }

    private void updatePositionMap() {
        if (links == null) return;

        int size = links.size();
        if (positionMap == null) {
            positionMap = new LinkedHashMap<>(size);
        } else {
            positionMap.clear();
        }
        for (int i = 0; i < size; i++) {
            Link link = links.get(i);
            positionMap.put(link.getId(), i);
        }
    } // updatePositionMap
}
