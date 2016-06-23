package com.tcl.downloader.sample.ui.fragment;

import android.app.Fragment;

import org.aisen.android.support.bean.TabItem;
import org.aisen.android.ui.fragment.ATabsTabLayoutFragment;

import java.util.ArrayList;

/**
 * Created by wangdan on 16/6/23.
 */
public class TabsFragment extends ATabsTabLayoutFragment<TabItem> {

    public static TabsFragment newInstance() {
        return new TabsFragment();
    }

    @Override
    protected ArrayList<TabItem> generateTabs() {
        ArrayList<TabItem> tabItems = new ArrayList<>();

        tabItems.add(new TabItem("1", "精品"));
        tabItems.add(new TabItem("2", "排行"));
        tabItems.add(new TabItem("3", "应用"));
        tabItems.add(new TabItem("4", "游戏"));

        return tabItems;
    }

    @Override
    protected Fragment newFragment(TabItem tabItem) {
        return AppsListFragment.newInstance(Integer.parseInt(tabItem.getType()));
    }

}
