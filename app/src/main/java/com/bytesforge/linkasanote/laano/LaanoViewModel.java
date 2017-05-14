package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;

import com.bytesforge.linkasanote.databinding.DrawerHeaderBinding;

import static com.google.common.base.Preconditions.checkNotNull;

public class LaanoViewModel extends BaseObservable {

    private final Context context;

    @Bindable
    public LaanoDrawerHeaderViewModel headerViewModel;

    public LaanoViewModel(@NonNull Context context) {
        this.context = checkNotNull(context);
        headerViewModel = new LaanoDrawerHeaderViewModel(context);
    }

    @BindingAdapter({"headerViewModel"})
    public static void setupDrawerHeader(NavigationView view, LaanoDrawerHeaderViewModel viewModel) {
        DrawerHeaderBinding binding = DrawerHeaderBinding.inflate(
                LayoutInflater.from(view.getContext()), view, false);
        binding.setViewModel(viewModel);
        binding.executePendingBindings();
        view.addHeaderView(binding.getRoot());
    }

    public void setInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            applyInstanceState(getDefaultInstanceState());
        } else {
            applyInstanceState(savedInstanceState);
        }
        headerViewModel.setInstanceState(savedInstanceState);
    }

    public void saveInstanceState(@NonNull Bundle outState) {
        checkNotNull(outState);
        headerViewModel.saveInstanceState(outState);
    }

    public void applyInstanceState(@NonNull Bundle state) {
        checkNotNull(state);
        notifyChange();
    }

    public Bundle getDefaultInstanceState() {
        return new Bundle();
    }
}
