package com.example.jack.brainwaves;

import android.app.ActionBar;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.astuetz.PagerSlidingTabStrip;
import com.example.jack.brainwaves.fragments.HomeScoreFragment;
import com.example.jack.brainwaves.fragments.MuseFragment;
import com.example.jack.brainwaves.fragments.PSSFragment;
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

        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //changeColor(currentColor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

//            case R.id.action_contact:
//                QuickContactFragment dialog = new QuickContactFragment();
//                dialog.show(getSupportFragmentManager(), "QuickContactFragment");
//                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void changeColor(int newColor) {

        tabs.setIndicatorColor(newColor);

        // change ActionBar color just if an ActionBar is available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            Drawable colorDrawable = new ColorDrawable(newColor);
            Drawable bottomDrawable = getResources().getDrawable(R.drawable.actionbar_bottom);
            LayerDrawable ld = new LayerDrawable(new Drawable[]{colorDrawable, bottomDrawable});

            if (oldBackground == null) {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    ld.setCallback(drawableCallback);
                } else {
                    getActionBar().setBackgroundDrawable(ld);
                }

            } else {

                TransitionDrawable td = new TransitionDrawable(new Drawable[]{oldBackground, ld});

                // workaround for broken ActionBarContainer drawable handling on
                // pre-API 17 builds
                // https://github.com/android/platform_frameworks_base/commit/a7cc06d82e45918c37429a59b14545c6a57db4e4
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    td.setCallback(drawableCallback);
                } else {
                    getActionBar().setBackgroundDrawable(td);
                }

                td.startTransition(200);

            }

            oldBackground = ld;

            // http://stackoverflow.com/questions/11002691/actionbar-setbackgrounddrawable-nulling-background-from-thread-handler
            ActionBar ab = getActionBar();
            ab.setDisplayShowTitleEnabled(false);
            ab.setDisplayShowTitleEnabled(true);

        }

        currentColor = newColor;

    }

    public void onColorClicked(View v) {

        int color = Color.parseColor(v.getTag().toString());
        //changeColor(color);

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
                default:
                    return SuperAwesomeCardFragment.newInstance(position);
            }
        }

    }

}