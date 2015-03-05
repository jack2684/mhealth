package com.example.jack.brainwaves.fragments;

import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jack.brainwaves.R;
import com.example.jack.brainwaves.helper.OrientationHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Created by jack on 3/2/15.
 */
public class PSSFragment extends SuperAwesomeCardFragment implements View.OnClickListener {
    // Key to the show
    private float normClassifierOutput;     // Normalized, expected to be 0 to 1
    private boolean showingPSS = false;

    // Constants
    static final int ANIM_DURATION = 3600;
    static final int SLEEP_INTER = 100;
    static final int CLEAR_DURATION = 1200;
    private static final Boolean SCORE_TYPE1 = true;
    private static final Boolean SCORE_TYPE2 = false;

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
            "In the last month, how often have you felt difficulties were piling up so high that you could not overcome them?",
    };

    private static Boolean PSSScoreType[] = {
            SCORE_TYPE1,
            SCORE_TYPE1,
            SCORE_TYPE1,
            SCORE_TYPE2,
            SCORE_TYPE2,
            SCORE_TYPE1,
            SCORE_TYPE2,
            SCORE_TYPE2,
            SCORE_TYPE1,
            SCORE_TYPE1,
    };

    // Views
    private TextView pssText;
    private TextView stressScoreTextView;
    private FrameLayout welcomFrame;
    private RadioGroup pssRadioGroup;
    private int currentPssTextIndex;
    private String pssResponses = "";

    ScoreTextAnimationHelper scoreAnimator;

    public static PSSFragment newInstance(int position) {
        PSSFragment f = new PSSFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        this.inflater = inflater;
        this.container = container;
        mMainActivity = getActivity();
        isLandscape = OrientationHelper.isLandsacpe(mMainActivity);
        inflateLayout2Fragment(R.layout.fragment_pss);

        // Create listeners and pass reference to activity to them
        welcomFrame = (FrameLayout) findViewById(R.id.pss_welcome);
        stressScoreTextView = (TextView) findViewById(R.id.stressScoreTextView);
        welcomFrame.setOnClickListener(this);
        TextView button;
        button = (TextView) findViewById(R.id.pss_quit);
        button.setOnClickListener(this);
        button = (TextView) findViewById(R.id.pss_next);
        button.setOnClickListener(this);
        pssText = (TextView) findViewById(R.id.pss_text);
        pssText.setText(PSSText[currentPssTextIndex]);
        pssRadioGroup = (RadioGroup) findViewById(R.id.pss_radio_group);

        scoreAnimator = new ScoreTextAnimationHelper();

        return mMainView;
    }

    @Override
    public void onResume() {
        super.onResume();
        normClassifierOutput = 0f;
        pssRadioGroup.clearCheck();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pss_next) {
            View checkedRadio = pssRadioGroup.findViewById(pssRadioGroup.getCheckedRadioButtonId());
            if(checkedRadio == null) {
                Toast.makeText(mMainActivity, "Please make an option.", Toast.LENGTH_SHORT).show();
                return;
            }
            int pssResponse = pssRadioGroup.indexOfChild(checkedRadio);
            normClassifierOutput += getThisPSSScore(pssResponse, currentPssTextIndex);
            pssResponses += pssResponse + (currentPssTextIndex == PSSText.length - 1 ? "" : ",") ;

            currentPssTextIndex = (currentPssTextIndex + 1) % PSSText.length;
            pssText.setText(PSSText[currentPssTextIndex]);
            pssText.invalidate();
            pssRadioGroup.clearCheck();
            if (currentPssTextIndex == 0) {
//                writePssResponses();
                pssResponses = "";
                Toast.makeText(mMainActivity, "Writing PSS Responses", Toast.LENGTH_SHORT).show();
                welcomFrame.setVisibility(View.VISIBLE);
                updateNormClassifierOutput();
            }
        } else if (v.getId() == R.id.pss_quit) {
            welcomFrame.setVisibility(View.VISIBLE);
            updateScoreTextViewInUiThread("TAP");
            normClassifierOutput = 0f;
            currentPssTextIndex = 0;
        } else if (v.getId() == R.id.pss_welcome && showingPSS == false) {
            welcomFrame.setVisibility(View.GONE);
        }
    }

    protected float getThisPSSScore (int res, int idx) {
        int N = pssRadioGroup.getChildCount();
        return (PSSScoreType[idx] ? N - res : res ) / ((float) N * PSSText.length);
    }

    protected void updateNormClassifierOutput() {
        // @TODO: this is just demo data, will replace with realworld data later
        scoreAnimator.stopThread();
        Thread myThread = new Thread(scoreAnimator);
        myThread.start();
    }

    private void writePssResponses() {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        //@TODO implement session name across fragments
        File outFile = new File(root + "/pss_" + ("jacktest" + "_" + System.currentTimeMillis()));
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

    protected class ScoreTextAnimationHelper implements Runnable {
        // Circular animation
        private boolean keepRunning = false;
        private float digit;

        public void stopThread() {
            keepRunning = false;
        }

        //@Override
        public void run() {
            try {
                showingPSS = true;
                keepRunning = true;
                digit = 0.f;
                float step = SLEEP_INTER * normClassifierOutput / ANIM_DURATION;
                while(digit <= normClassifierOutput && keepRunning) {
                    Thread.sleep(SLEEP_INTER);
                    digit += step;
                    updateScoreTextViewInUiThread(
                            String.format("%.0f", Math.floor(digit * 100)));    // This has to be done in UI Thread!!
                }
                showingPSS = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void updateScoreTextViewInUiThread(final String s) {
        stressScoreTextView.post(new Runnable() {
            @Override
            public void run() {
                stressScoreTextView.setText(s);
            }
        });
    }
}
