package com.bytesforge.linkasanote.laano.favorites;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.databinding.ItemFavoritesBinding;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private final FavoritesContract.Presenter presenter;
    private final FavoritesViewModel viewModel;

    private List<Favorite> favorites;
    private SparseBooleanArray selectedIds;

    public FavoritesAdapter(
            @NonNull List<Favorite> favorites,
            @NonNull FavoritesContract.Presenter presenter,
            @NonNull FavoritesViewModel viewModel) {
        this.favorites = checkNotNull(favorites);
        this.presenter = checkNotNull(presenter);
        this.viewModel = checkNotNull(viewModel);
        selectedIds = new SparseBooleanArray();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemFavoritesBinding binding;

        public ViewHolder(ItemFavoritesBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(
                Favorite favorite, FavoritesContract.Presenter presenter,
                FavoritesViewModel viewModel, Integer position) {
            binding.setFavorite(favorite);
            binding.setPresenter(presenter);
            binding.setViewModel(viewModel);
            binding.setPosition(position);

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
        holder.bind(favorite, presenter, viewModel, position);
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    // Selection

    public void toggleSelection(int position) {
        boolean isSelected = selectedIds.get(position);
        if (isSelected) {
            selectedIds.delete(position);
        } else {
            selectedIds.put(position, true);
        }
        notifyItemChanged(position);
    }

    public void removeSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public SparseBooleanArray getSelectedIds() {
        return selectedIds;
    }

    // Swap

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
