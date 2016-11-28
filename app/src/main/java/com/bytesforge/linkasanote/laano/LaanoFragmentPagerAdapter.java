package com.bytesforge.linkasanote.laano;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.BaseFragment;

import java.util.ArrayList;
import java.util.List;

public class LaanoFragmentPagerAdapter extends FragmentPagerAdapter {

    private final Context context;
    private List<BaseFragment> tabFragments;
    private int tabCount;

    public LaanoFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);

        this.context = context;
        tabFragments = new ArrayList<>();
        tabCount = tabFragments.size();
    }

    @Override
    public Fragment getItem(int position) {
        return tabFragments.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        tabFragments.set(position, null);
    }

    @Override
    public int getCount() {
        return tabCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabFragments.get(position).getTitle();
    }

    public void addTab(BaseFragment tabFragment, String title) {
        BaseFragment.attachTitle(title, tabFragment);

        tabFragments.add(tabFragment);
        tabCount = tabFragments.size();
    }

    public BaseFragment getFragment(int position) {
        return tabFragments.get(position);
    }
}
