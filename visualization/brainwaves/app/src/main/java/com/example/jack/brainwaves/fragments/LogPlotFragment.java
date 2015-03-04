package com.example.jack.brainwaves.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by jack on 3/2/15.
 */
public class LogPlotFragment extends SuperAwesomeCardFragment {
    public static LogPlotFragment newInstance(int position) {
        LogPlotFragment f = new LogPlotFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle savedInstanceState) {
        return createCardView("This is indeed a very cool app thanks to\n\n" +
                "Mridul Khan - Data analyst\n" +
                "Junjie Guan - App Dev\n" +
                "Donglin Wei - Experiment design\n\n" +
                "2015 Winter @ Dartmouth College :)");
    }
}
