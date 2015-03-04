package com.example.jack.brainwaves.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.example.jack.brainwaves.R;
import com.example.jack.brainwaves.helper.OrientationHelper;
import com.kyleduo.switchbutton.SwitchButton;

/**
 * Created by jack on 3/2/15.
 */
public class SettingFragment extends SuperAwesomeCardFragment {

    private SwitchButton mDemosb, mSyncsb;

    public static SettingFragment newInstance(int position) {
        SettingFragment f = new SettingFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        inflater = i;
        container = c;
        mMainActivity = getActivity();
        isLandscape = OrientationHelper.isLandsacpe(mMainActivity);
        inflateLayout2Fragment(R.layout.fragment_setting);

        // Init views
        mDemosb = (SwitchButton) findViewById(R.id.mDemosb);
        mSyncsb = (SwitchButton) findViewById(R.id.mSyncsb);
        mDemosb.setChecked(true);
        mSyncsb.setChecked(true);

        return mMainView;
    }
}
