package com.todoroo.astrid.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.viewpagerindicator.TitleProvider;

public class TaskEditViewPager extends FragmentPagerAdapter implements TitleProvider
{
    private static String[] titles = new String[]
                                          {
"Activity", "More", "Awesome"
                                          };
    public TaskEditActivity parent;

    public TaskEditViewPager(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount()
    {
        return titles.length;
    }

    @Override
    public Object instantiateItem( View pager, int position )
    {
        View pageView = parent.getPageView(position);

        ((ViewPager)pager).addView( pageView, 0 );
        return pageView;
    }


    @Override
    public String getTitle(int position) {
        return titles[position];
    }


    @Override
    public Fragment getItem(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}