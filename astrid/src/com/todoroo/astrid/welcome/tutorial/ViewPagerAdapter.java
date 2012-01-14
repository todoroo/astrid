package com.todoroo.astrid.welcome.tutorial;


import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;

public class ViewPagerAdapter extends PagerAdapter
{
    private static int[] images = new int[]
                                          {
                                              R.drawable.welcome_walkthrough_1,
                                              R.drawable.welcome_walkthrough_2,
                                              R.drawable.welcome_walkthrough_3,
                                              R.drawable.welcome_walkthrough_4,
                                              R.drawable.welcome_walkthrough_5,
                                              R.drawable.welcome_walkthrough_6,
                                              R.drawable.welcome_screen
                                          };
    private static String[] title = new String[]
                                          {
                                              "Welcome to Astrid!",
                                              "Make lists",
                                              "Share lists",
                                              "Divvy up tasks",
                                              "Provide details",
                                              "Discover",
                                              "Login"
                                          };
    private static String[] body = new String[]
                                          {
        "The perfect personal\nto-do list that works great\nwith friends",
        "Perfect for any list:\nto read, to watch, to buy,\nto visit, to do!",
        "Share lists\nwith friends, housemates,\nor your sweetheart!",
        "Never wonder who's\nbringing dessert!",
        "Tap to add notes,\nset reminders,\nand much more!",
        "Additional features,\nproductivity tips, and\nsuggestions from friends",
        "Login"
                                          };
    private static int[] layouts = new int[]
                                          {
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_login_page,

                                          };
    private final Context context;
    public WelcomeWalkthrough parent;

    public ViewPagerAdapter( Context context )
    {
        this.context = context;
    }


    @Override
    public int getCount()
    {
        return layouts.length;
    }

    @Override
    public Object instantiateItem( View pager, int position )
    {
        LayoutInflater inflater = LayoutInflater.from(context);

        View pageView = inflater.inflate(layouts[position], null, true);
        pageView.setLayoutParams( new ViewGroup.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));

        if (position != getCount()-1){
        ImageView imageView = (ImageView) pageView.findViewById(R.id.welcome_walkthrough_image);
        imageView.setImageResource(images[position]);
        ImageView fabricImage = (ImageView) pageView.findViewById(R.id.welcome_walkthrough_fabric);
        fabricImage.setScaleType(ImageView.ScaleType.FIT_XY);

        TextView titleView = (TextView) pageView.findViewById(R.id.welcome_walkthrough_title);
        titleView.setText(title[position]);
        TextView bodyView = (TextView) pageView.findViewById(R.id.welcome_walkthrough_body);
        bodyView.setText(body[position]);
        }

        ((ViewPager)pager).addView( pageView, 0 );
        parent.pageScrolled(position, pageView);
        return pageView;
    }

    @Override
    public void destroyItem( View pager, int position, Object view )
    {
        ((ViewPager)pager).removeView( (View)view );
    }

    @Override
    public boolean isViewFromObject( View view, Object object )
    {
        return view.equals( object );
    }

    @Override
    public void finishUpdate( View view ) {}

    @Override
    public void restoreState( Parcelable p, ClassLoader c ) {}

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void startUpdate( View view ) {}
}