package com.example.jack.brainwaves.fragments;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.appyvet.rangebar.RangeBar;
import com.example.jack.brainwaves.R;
import com.example.jack.brainwaves.helper.OrientationHelper;
import com.sefford.circularprogressdrawable.CircularProgressDrawable;

import java.util.Random;

/**
 * Created by jack on 3/2/15.
 */
public class HomeScoreFragment extends SuperAwesomeCardFragment {
    // Constants
    static final int ANIM_DURATION = 3600;
    static final int SLEEP_INTER = 100;
    static final int CLEAR_DURATION = 1200;

    // Views
    private TextView stressScoreTextView;
    private TextView durationTextView;
    private ImageView ivDrawable;
    private RangeBar durationSeekBar;
    private RangeBar durationSeekBarL;  // For landscape

    // Draweable
    private CircularProgressDrawable circularDrawable;

    // Key to the show
    private float normClassifierOutput;     // Normalized, expected to be 0 to 1
    private float dynamicPercentage;
    private Animator circularAnimater;
    private ScoreTextAnimation scoreAnimater;
    private Thread myThread;

    public static HomeScoreFragment newInstance(int position) {
        HomeScoreFragment f = new HomeScoreFragment();
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
        inflateLayout2Fragment(R.layout.fragment_score_circular, R.layout.fragment_score_circular_landscape);

        // initialize Views:
        stressScoreTextView = (TextView) findViewById(R.id.stressScoreTextView);
        durationTextView = (TextView) findViewById(R.id.durationTextView);
        ivDrawable = (ImageView) findViewById(R.id.iv_drawable);
        durationSeekBar = (RangeBar) findViewById(R.id.materialBar);
        durationSeekBar.setSelectorColor(getResources().getColor(android.R.color.darker_gray));
        durationSeekBar.setConnectingLineColor(getResources().getColor(android.R.color.darker_gray));
        durationSeekBar.setPinColor(getResources().getColor(android.R.color.darker_gray));
        durationSeekBar.setSeekPinByIndex(3);
        translateProgress2Duration();

        scoreAnimater = new ScoreTextAnimation();
        circularDrawable = new CircularProgressDrawable.Builder()
                .setRingWidth(getResources().getDimensionPixelSize(R.dimen.drawable_ring_size))
                .setOutlineColor(getResources().getColor(android.R.color.darker_gray))
                .setRingColor(getResources().getColor(android.R.color.holo_green_light))
                .create();
        ivDrawable.setImageDrawable(circularDrawable);

        durationSeekBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
                                              int rightPinIndex,
                                              String leftPinValue, String rightPinValue) {
                scoreAnimater.stopThread();
                clearAnimation();
                translateProgress2Duration();
                updateScoreTextViewInUiThread("TAP");
            }
        });

        stressScoreTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateNormClassifierOutput();
            }
        });


        return mMainView;
    }

    protected void updateNormClassifierOutput() {
        // @TODO: this is just demo data, will replace with realworld data later
        int ridx = durationSeekBar.getRightIndex();
        int cnt = durationSeekBar.getTickCount();
        normClassifierOutput = (float) (.9 * ((ridx + 3) % cnt) / cnt + 0.1f);
        if(ridx == 0) {
            Random rand = new Random();
            normClassifierOutput = rand.nextInt(100) / 100f;
        }
        scoreAnimater.stopThread();
        circularAnimation();
        myThread = new Thread(scoreAnimater);
        myThread.start();
    }

    /**
     * circularAnimation will fill the outer ring while applying a color effect from red to green
     *
     * @return Animation
     */
    private void circularAnimation() {
        AnimatorSet animation = new AnimatorSet();

        ObjectAnimator progressAnimation = ObjectAnimator.ofFloat(
                circularDrawable,
                CircularProgressDrawable.PROGRESS_PROPERTY,
                0f, normClassifierOutput);
        progressAnimation.setDuration(ANIM_DURATION);
        progressAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator colorAnimator = setDynamicColorsArguement();
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setDuration(ANIM_DURATION);

        animation.playTogether(progressAnimation, colorAnimator);
        try {
            animation.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * clearAnimation will turn a 3/4 animation with Anticipate/Overshoot interpolation to a
     * blank waiting - like state, wait for 2 seconds then return to the original state
     *
     * @return Animation
     */
    private void clearAnimation() {
        float progress = circularDrawable.getProgress();
        if(progress != 0f) {
            AnimatorSet animation = new AnimatorSet();
            ObjectAnimator progressAnimation = ObjectAnimator.ofFloat(
                    circularDrawable,
                    CircularProgressDrawable.PROGRESS_PROPERTY,
                    progress, 0f);
            normClassifierOutput = 0f;
            progressAnimation.setDuration(CLEAR_DURATION);
            progressAnimation.setInterpolator(new AnticipateInterpolator());
            animation.play(progressAnimation);
            try {
                animation.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ObjectAnimator setDynamicColorsArguement() {
        ObjectAnimator colorAnimator;
        if (normClassifierOutput < 0.16) {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light));
        } else if (normClassifierOutput < 0.33) {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_blue_light));
        } else if (normClassifierOutput < 0.5) {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_blue_light),
                    getResources().getColor(android.R.color.holo_purple));
        } else if (normClassifierOutput < 0.66) {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_blue_light),
                    getResources().getColor(android.R.color.holo_purple),
                    getResources().getColor(android.R.color.holo_orange_light));
        } else if (normClassifierOutput < 0.95) {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_blue_light),
                    getResources().getColor(android.R.color.holo_purple),
                    getResources().getColor(android.R.color.holo_orange_light),
                    getResources().getColor(android.R.color.holo_red_dark));
        } else {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_blue_light),
                    getResources().getColor(android.R.color.holo_purple),
                    getResources().getColor(android.R.color.holo_orange_light),
                    getResources().getColor(android.R.color.holo_red_dark),
                    getResources().getColor(android.R.color.black)
                    );
        }
    }

    protected class ScoreTextAnimation implements Runnable {
        // Circular animation
        private boolean keepRunning = false;

        public void stopThread() {
            keepRunning = false;
        }

        //@Override
        public void run() {
            try {
                keepRunning = true;
                dynamicPercentage = 0.f;
                float step = SLEEP_INTER * normClassifierOutput / ANIM_DURATION;
                while(dynamicPercentage <= normClassifierOutput && keepRunning) {
                    Thread.sleep(SLEEP_INTER);
                    dynamicPercentage += step;
                    updateScoreTextViewInUiThread(
                            String.format("%.0f", Math.floor(dynamicPercentage * 100)));    // This has to be done in UI Thread!!
                }
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

    protected void translateProgress2Duration() {
        final int progress = durationSeekBar.getRightIndex();
        durationTextView.post(new Runnable() {
            @Override
            public void run() {
                String base = "past";
                switch (progress) {
                    case 1:
                        durationTextView.setText(base + " 1 day");
                        break;
                    case 2:
                        durationTextView.setText(base + " 1 week");
                        break;
                    case 3:
                        durationTextView.setText(base + " 2 weeks");
                        break;
                    case 4:
                        durationTextView.setText(base + " 1 month");
                        break;
                    case 5:
                        durationTextView.setText(base + " 1 season");
                        break;
                    case 6:
                        durationTextView.setText(base + " 1/2 year");
                        break;
                    case 7:
                        durationTextView.setText(base + " 1 year");
                        break;
                    default:
                        durationTextView.setText("now");
                }
            }
        });
    }
}
