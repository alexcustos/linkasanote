package com.bytesforge.linkasanote.addeditfavorite;

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.databinding.FragmentAddEditFavoriteBinding;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class AddEditFavoriteFragment extends Fragment implements AddEditFavoriteContract.View {

    public static final String ARGUMENT_EDIT_FAVORITE_ID = "EDIT_FAVORITE_ID";

    private AddEditFavoriteContract.Presenter presenter;
    private AddEditFavoriteContract.ViewModel viewModel;

    private List<Tag> tags;

    public static AddEditFavoriteFragment newInstance() {
        return new AddEditFavoriteFragment();
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
    public void setPresenter(@NonNull AddEditFavoriteContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setViewModel(@NonNull AddEditFavoriteContract.ViewModel viewModel) {
        this.viewModel = checkNotNull(viewModel);
    }

    @Override
    public void finishActivity() {
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentAddEditFavoriteBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_add_edit_favorite, container, false);
        binding.setViewModel((AddEditFavoriteViewModel) viewModel);

        // FavoriteTags
        FavoriteTagsCompletionView completionView = binding.favoriteTags;
        if (completionView != null) {
            completionView.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Select);
            completionView.setDeletionStyle(TokenCompleteTextView.TokenDeleteStyle.SelectThenDelete);
            completionView.allowCollapse(false);
            char[] splitChars = {',', ';', ' '};
            completionView.setSplitChar(splitChars);
            completionView.allowDuplicates(false);
            completionView.performBestGuess(false);
            int threshold = getContext().getResources().getInteger(R.integer.tags_autocomplete_threshold);
            completionView.setThreshold(threshold);
            tags = new ArrayList<>();
            ArrayAdapter<Tag> adapter = new ArrayAdapter<>(
                    getContext(), android.R.layout.simple_list_item_1, tags);
            completionView.setAdapter(adapter);
            viewModel.setTagsCompletionView(completionView);
        }
        return binding.getRoot();
    }

    @Override
    public void swapTagsCompletionViewItems(List<Tag> tags) {
        if (this.tags != null) {
            this.tags.clear();
            this.tags.addAll(tags);
        }
    }
}
