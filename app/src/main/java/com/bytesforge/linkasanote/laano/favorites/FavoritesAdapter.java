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

package com.bytesforge.linkasanote.laano.favorites;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.databinding.ItemFavoritesBinding;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private static final String TAG = FavoritesAdapter.class.getSimpleName();

    private final FavoritesContract.Presenter presenter;
    private final FavoritesViewModel viewModel;

    @NonNull
    private List<Favorite> favorites;

    @NonNull
    private List<String> favoriteIds;

    public FavoritesAdapter(
            @NonNull List<Favorite> favorites,
            @NonNull FavoritesContract.Presenter presenter,
            @NonNull FavoritesViewModel viewModel) {
        this.favorites = checkNotNull(favorites);
        this.presenter = checkNotNull(presenter);
        this.viewModel = checkNotNull(viewModel);
        favoriteIds = new ArrayList<>(favorites.size());
        updateIds();
        setHasStableIds(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemFavoritesBinding binding;

        public ViewHolder(ItemFavoritesBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(
                Favorite favorite, FavoritesContract.Presenter presenter,
                FavoritesViewModel viewModel) {
            binding.setFavorite(favorite);
            binding.setPresenter(presenter);
            binding.setViewModel(viewModel); // NOTE: global viewModel for fragment and all items

            binding.executePendingBindings();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemFavoritesBinding binding = ItemFavoritesBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Favorite favorite = favorites.get(position);
        holder.bind(favorite, presenter, viewModel);
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    @Override
    public long getItemId(int position) {
        return favorites.get(position).getRowId();
    }

    // Items

    @NonNull
    public List<Favorite> getFavorites() {
        return favorites;
    }

    @NonNull
    public String[] getIds() {
        return favoriteIds.toArray(new String[favoriteIds.size()]);
    }

    public void swapItems(@NonNull List<Favorite> favorites) {
        this.favorites = checkNotNull(favorites);
        updateIds();
        notifyDataSetChanged();
    }

    public synchronized  void addItem(@NonNull Favorite favorite) {
        checkNotNull(favorite);
        favorites.add(favorite);
        favoriteIds.add(favorite.getId());
        notifyItemInserted(favorites.size() - 1);
    }

    public synchronized void addItems(@NonNull List<Favorite> favorites) {
        checkNotNull(favorites);
        final int start = this.favorites.size();
        this.favorites.addAll(favorites);
        for (Favorite favorite : favorites) {
            favoriteIds.add(favorite.getId());
        }
        notifyItemRangeInserted(start, favorites.size());
    }

    @Nullable
    public synchronized Favorite removeItem(int position) {
        Favorite favorite;
        try {
            favorite = favorites.remove(position);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        favoriteIds.remove(position);
        notifyItemRemoved(position);
        return favorite;
    }

    public synchronized int removeItem(@NonNull String id) {
        checkNotNull(id);
        int position = getPosition(id);
        if (position < 0) return -1;

        favorites.remove(position);
        favoriteIds.remove(position);
        notifyItemRemoved(position);
        return position;
    }

    public synchronized void clear() {
        favorites.clear();
        favoriteIds.clear();
        notifyDataSetChanged();
    }

    public int getPosition(@Nullable String favoriteId) {
        if (favoriteId == null) return -1;

        return favoriteIds.indexOf(favoriteId);
    }

    public void notifyItemChanged(String favoriteId) {
        int position = getPosition(favoriteId);
        if (position < 0) return;

        notifyItemChanged(position);
    }

    @Nullable
    public String getId(int position) {
        try {
            return favorites.get(position).getId();
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private synchronized void updateIds() {
        favoriteIds.clear();
        for (int i = 0; i < favorites.size(); i++) {
            Favorite favorite = favorites.get(i);
            favoriteIds.add(i, favorite.getId());
        }
    }
}
