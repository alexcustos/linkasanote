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

package com.bytesforge.linkasanote.laano.links;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.databinding.ItemLinksBinding;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class LinksAdapter extends RecyclerView.Adapter<LinksAdapter.ViewHolder> {

    private static final String TAG = LinksAdapter.class.getSimpleName();

    private final LinksContract.Presenter presenter;
    private final LinksViewModel viewModel;

    @NonNull
    private List<Link> links;

    @NonNull
    private List<String> linkIds;

    public LinksAdapter(
            @NonNull List<Link> links, @NonNull LinksContract.Presenter presenter,
            @NonNull LinksViewModel viewModel) {
        this.links = checkNotNull(links);
        this.presenter = checkNotNull(presenter);
        this.viewModel = checkNotNull(viewModel);
        linkIds = new ArrayList<>(links.size());
        updateIds();
        setHasStableIds(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLinksBinding binding;

        public ViewHolder(ItemLinksBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Link link, LinksContract.Presenter presenter, LinksViewModel viewModel) {
            binding.setLink(link);
            binding.setPresenter(presenter);
            binding.setViewModel(viewModel); // NOTE: global viewModel for fragment and all items

            RecyclerView rvLinkNotes = binding.rvLinkNotes;
            Context context = rvLinkNotes.getContext();
            // NOTE: custom divider is required because addItemDecoration extends a view dynamically
            rvLinkNotes.setHasFixedSize(true);
            rvLinkNotes.setNestedScrollingEnabled(false);
            LinearLayoutManager rvLayoutManager = new LinearLayoutManager(context);
            rvLinkNotes.setLayoutManager(rvLayoutManager);
            rvLinkNotes.setAdapter(new LinksNotesAdapter(link.getNotes(), presenter));
            rvLinkNotes.setLayoutFrozen(true); // NOTE: after setAdapter
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
    public List<Link> getFavorites() {
        return links;
    }

    @NonNull
    public String[] getIds() {
        return linkIds.toArray(new String[linkIds.size()]);
    }

    public void swapItems(@NonNull List<Link> links) {
        this.links = checkNotNull(links);
        updateIds();
        notifyDataSetChanged();
    }

    public synchronized  void addItem(@NonNull Link link) {
        checkNotNull(link);
        links.add(link);
        linkIds.add(link.getId());
        notifyItemInserted(links.size() - 1);
    }

    public synchronized void addItems(@NonNull List<Link> links) {
        checkNotNull(links);
        final int start = this.links.size();
        this.links.addAll(links);
        for (Link link : links) {
            linkIds.add(link.getId());
        }
        notifyItemRangeInserted(start, links.size());
    }

    @Nullable
    public synchronized Link removeItem(int position) {
        Link link;
        try {
            link = links.remove(position);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        linkIds.remove(position);
        notifyItemRemoved(position);
        return link;
    }

    public synchronized int removeItem(@NonNull String id) {
        checkNotNull(id);
        int position = getPosition(id);
        if (position < 0) return -1;

        links.remove(position);
        linkIds.remove(position);
        notifyItemRemoved(position);
        return position;
    }

    public synchronized void clear() {
        links.clear();
        linkIds.clear();
        notifyDataSetChanged();
    }

    public int getPosition(@Nullable String linkId) {
        if (linkId == null) return -1;

        return linkIds.indexOf(linkId);
    }

    public void notifyItemChanged(String linkId) {
        int position = getPosition(linkId);
        if (position < 0) return;

        notifyItemChanged(position);
    }

    @Nullable
    public String getId(int position) {
        try {
            return links.get(position).getId();
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private synchronized void updateIds() {
        linkIds.clear();
        for (int i = 0; i < links.size(); i++) {
            Link link = links.get(i);
            linkIds.add(i, link.getId());
        }
    }
}
