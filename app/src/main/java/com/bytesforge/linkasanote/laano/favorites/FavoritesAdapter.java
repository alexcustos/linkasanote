package com.bytesforge.linkasanote.laano.favorites;

import android.content.Context;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.databinding.ItemFavoritesBinding;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private List<Favorite> favorites;
    private Context context;

    public FavoritesAdapter(Context context, List<Favorite> favorites) {
        this.context = context;
        this.favorites = favorites;
    }

    public Context getContext() {
        return context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemFavoritesBinding binding;

        public ViewHolder(ItemFavoritesBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Favorite favorite) {
            binding.setFavorite(favorite);
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
        holder.bind(favorite);
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    public void swapItems(List<Favorite> favorites) {
        final FavoriteDiffCallback diffCallback =
                new FavoriteDiffCallback(this.favorites, favorites);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.favorites.clear();
        this.favorites.addAll(favorites);

        diffResult.dispatchUpdatesTo(this);
    }

    public class FavoriteDiffCallback extends DiffUtil.Callback {

        private List<Favorite> oldList;
        private List<Favorite> newList;

        public FavoriteDiffCallback(List<Favorite> oldList, List<Favorite> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId()
                    .equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Favorite oldFavorite = oldList.get(oldItemPosition);
            Favorite newFavorite = newList.get(newItemPosition);

            return oldFavorite.getName().equals(newFavorite.getName())
                    && oldFavorite.getTagsAsString().equals(newFavorite.getTagsAsString());
        }
    }
}
