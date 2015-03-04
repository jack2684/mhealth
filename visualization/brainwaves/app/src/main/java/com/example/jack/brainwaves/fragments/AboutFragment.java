package com.example.jack.brainwaves.fragments;

import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jack.brainwaves.R;
import com.example.jack.brainwaves.helper.OrientationHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by jack on 3/2/15.
 */
public class AboutFragment extends SuperAwesomeCardFragment {
    public static AboutFragment newInstance(int position) {
        AboutFragment f = new AboutFragment();
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
