package com.bytesforge.linkasanote.laano.favorites;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.databinding.ItemFavoritesBinding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private static final String TAG = FavoritesAdapter.class.getSimpleName();

    private final FavoritesContract.Presenter presenter;
    private final FavoritesViewModel viewModel;

    private List<Favorite> favorites;
    private Map<String, Integer> positionMap;

    public FavoritesAdapter(
            @NonNull List<Favorite> favorites,
            @NonNull FavoritesContract.Presenter presenter,
            @NonNull FavoritesViewModel viewModel) {
        this.favorites = checkNotNull(favorites);
        this.presenter = checkNotNull(presenter);
        this.viewModel = checkNotNull(viewModel);
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
    public Favorite removeItem(int position) {
        Favorite favorite = favorites.remove(position);
        updatePositionMap();
        notifyItemRemoved(position);
        return favorite;
    }

    public void swapItems(@NonNull List<Favorite> favorites) {
        checkNotNull(favorites);

        this.favorites = favorites;
        updatePositionMap();
        notifyDataSetChanged();
    }

    public int getPosition(@Nullable String favoriteId) {
        if (favoriteId == null) return -1;

        Integer position = positionMap.get(favoriteId);
        if (position == null) {
            Log.e(TAG, "No position is found for Favorite [" + favoriteId + "]");
            return -1;
        }
        return position;
    }

    private void updatePositionMap() {
        if (favorites == null) return;

        int size = favorites.size();
        if (positionMap == null) {
            positionMap = new LinkedHashMap<>(size);
        } else {
            positionMap.clear();
        }
        for (int i = 0; i < size; i++) {
            Favorite favorite = favorites.get(i);
            positionMap.put(favorite.getId(), i);
        }
    } // updatePositionMap
}
