package com.example.jack.brainwaves.fragments;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jack.brainwaves.R;
import com.example.jack.brainwaves.helper.OrientationHelper;
import com.interaxon.libmuse.Accelerometer;
import com.interaxon.libmuse.ConnectionState;
import com.interaxon.libmuse.Eeg;
import com.interaxon.libmuse.LibMuseVersion;
import com.interaxon.libmuse.Muse;
import com.interaxon.libmuse.MuseArtifactPacket;
import com.interaxon.libmuse.MuseConnectionListener;
import com.interaxon.libmuse.MuseConnectionPacket;
import com.interaxon.libmuse.MuseDataListener;
import com.interaxon.libmuse.MuseDataPacket;
import com.interaxon.libmuse.MuseDataPacketType;
import com.interaxon.libmuse.MuseManager;
import com.interaxon.libmuse.MusePreset;
import com.interaxon.libmuse.MuseVersion;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jack on 3/2/15.
 */
public class PSSFragment extends SuperAwesomeCardFragment implements View.OnClickListener {
    private TextView pssText;
    private RadioGroup pssRadioGroup;
    private int currentPssTextIndex;
    private String pssResponses = "";

    public static PSSFragment newInstance(int position) {
        PSSFragment f = new PSSFragment();
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
        inflateLayout2Fragment(R.layout.perceived_stress_scale);

        // Create listeners and pass reference to activity to them
        Button button;
        button = (Button) findViewById(R.id.pss_next);
        button.setOnClickListener(this);
        pssText = (TextView) findViewById(R.id.pss_text);
        pssText.setText(PSSText[currentPssTextIndex]);
        pssRadioGroup = (RadioGroup) findViewById(R.id.pss_radio_group);
        ((RadioButton) findViewById(R.id.radio_never)).setChecked(true);
        return mMainView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pss_next) {
            int pssResponse = pssRadioGroup.indexOfChild(
                    pssRadioGroup.findViewById(pssRadioGroup.getCheckedRadioButtonId())
            );
            pssResponses += pssResponse + (currentPssTextIndex == PSSText.length - 1 ? "" : ",") ;

            currentPssTextIndex = (currentPssTextIndex + 1) % PSSText.length;
            pssText.setText(PSSText[currentPssTextIndex]);
            pssText.invalidate();
            ((RadioButton) findViewById(R.id.radio_never)).setChecked(true);
            if (currentPssTextIndex == 0) {
                writePssResponses();
                pssResponses = "";
                Toast.makeText(mMainActivity, "Writing PSS Responses", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void writePssResponses() {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File outFile = new File(root + "/pss_" + ((EditText) findViewById(R.id.sessionName)).getText() + "_" + System.currentTimeMillis());
        try {
            outFile.createNewFile();
            FileOutputStream writer = new FileOutputStream(outFile);

            writer.write(pssResponses.getBytes());
            writer.flush();

            writer.close();
            MediaScannerConnection.scanFile(mMainActivity, new String[]{outFile.getAbsolutePath()}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String PSSText[] = {
            "In the last month, how often have you been upset because of something that happened unexpectedly?",
            "In the last month, how often have you felt that you were unable to control the important things in your life?",
            "In the last month, how often have you felt nervous and \"stressed\"?",
            "In the last month, how often have you felt confident about your ability to handle your personal problems?",
            "In the last month, how often have you felt that things were going your way?",
            "In the last month, how often have you found that you could not cope with all the things that you had to do?",
            "In the last month, how often have you been able to control irritations in your life?",
            "In the last month, how often have you felt that you were on top of things?",
            "In the last month, how often have you been angered because of things that were outside of your control?",
            "In the last month, how often have you felt difficulties were piling up so high that you could not overcome them?"
    };
}
