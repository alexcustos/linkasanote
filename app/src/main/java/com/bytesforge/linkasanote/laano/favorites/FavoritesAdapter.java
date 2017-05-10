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

    private List<Favorite> favorites;
    private List<String> favoritesIds;

    public FavoritesAdapter(
            @NonNull List<Favorite> favorites,
            @NonNull FavoritesContract.Presenter presenter,
            @NonNull FavoritesViewModel viewModel) {
        this.favorites = checkNotNull(favorites);
        this.presenter = checkNotNull(presenter);
        this.viewModel = checkNotNull(viewModel);
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
        return favoritesIds.toArray(new String[favoritesIds.size()]);
    }

    public void swapItems(@NonNull List<Favorite> favorites) {
        checkNotNull(favorites);
        this.favorites = favorites;
        updateIds();
        notifyDataSetChanged();
    }

    @Nullable
    public synchronized Favorite removeItem(int position) {
        Favorite favorite;
        try {
            favorite = favorites.remove(position);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        favoritesIds.remove(position);
        notifyItemRemoved(position);
        return favorite;
    }

    public synchronized int removeItem(@NonNull String id) {
        checkNotNull(id);
        int position = getPosition(id);
        if (position < 0) return -1;

        favorites.remove(position);
        favoritesIds.remove(position);
        notifyItemRemoved(position);
        return position;
    }

    public int getPosition(@Nullable String favoriteId) {
        if (favoriteId == null) {
            return -1;
        }
        return favoritesIds.indexOf(favoriteId);
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
        int size = favorites.size();
        if (favoritesIds == null) {
            favoritesIds = new ArrayList<>(size);
        } else {
            favoritesIds.clear();
        }
        for (int i = 0; i < size; i++) {
            Favorite favorite = favorites.get(i);
            favoritesIds.add(i, favorite.getId());
        }
    }
}
