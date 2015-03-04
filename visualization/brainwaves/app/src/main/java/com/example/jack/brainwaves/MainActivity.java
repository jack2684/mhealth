package com.example.jack.brainwaves;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Menu;

import com.astuetz.PagerSlidingTabStrip;
import com.example.jack.brainwaves.fragments.AboutFragment;
import com.example.jack.brainwaves.fragments.HomeScoreFragment;
import com.example.jack.brainwaves.fragments.MuseFragment;
import com.example.jack.brainwaves.fragments.PSSFragment;
import com.example.jack.brainwaves.fragments.SettingFragment;
import com.example.jack.brainwaves.fragments.SuperAwesomeCardFragment;

public class MainActivity extends FragmentActivity {

    private final static String[] TITLES = {
            "About",
            "Settings",
            "PSS Questionnaires",
            "Home",
            "Log over time",
            "Real time plot",
            "Muse Config",
    };

    final static int ABOUT      = 0;
    final static int SETTING    = 1;
    final static int PSS        = 2;
    final static int HOME       = 3;
    final static int LOGPLOT    = 4;
    final static int REALTIME   = 5;
    final static int MUSE       = 6;
    final static int START_PAGE = HOME;

    private final Handler handler = new Handler();

    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private MyPagerAdapter adapter;

    private Drawable oldBackground = null;
    private int currentColor = 0xFF666666;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new MyPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                .getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        pager.setCurrentItem(START_PAGE, true);
        tabs.setViewPager(pager);

        //changeColor(currentColor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentColor", currentColor);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentColor = savedInstanceState.getInt("currentColor");
        //changeColor(currentColor);
    }

    private Drawable.Callback drawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            getActionBar().setBackgroundDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            handler.postAtTime(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            handler.removeCallbacks(what);
        }
    };

    public class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        /*@TODO: return different fragment based on the postiion
            Apparently onCreateView in Fragment is corresponding to onCreate in Activity
         */
        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case HOME:
                    return HomeScoreFragment.newInstance(position);
                case MUSE:
                    return MuseFragment.newInstance(position);
                case PSS:
                    return PSSFragment.newInstance(position);
                case ABOUT:
                    return AboutFragment.newInstance(position);
                case SETTING:
                    return SettingFragment.newInstance(position);
                default:
                    return SuperAwesomeCardFragment.newInstance(position);
            }
        }

    }

}