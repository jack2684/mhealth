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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.jack.brainwaves.R;
import com.example.jack.brainwaves.helper.OrientationHelper;
import com.sefford.circularprogressdrawable.CircularProgressDrawable;

import java.util.Random;

/**
 * Created by jack on 3/2/15.
 */
public class HomeScoreFragment extends Fragment {
    // Constants
    static final int ANIM_DURATION = 3600;
    static final int SLEEP_INTER = 100;

    // Swipe pager related
    protected View mMainView;
    protected Activity mMainActivity;
    private static final String ARG_POSITION = "position";
    private int position;

    // Fragment view Control
    private boolean firstTime;
    private boolean isLandscape;

    // Views
    private TextView stressScoreTextView;
    private TextView durationTextView;
    private SeekBar durationSeekBar;
    private CircularProgressDrawable circularDrawable;
    private ImageView ivDrawable;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainActivity = getActivity();
        isLandscape = OrientationHelper.isLandsacpe(mMainActivity);
        if (isLandscape) {
            mMainView = inflater.inflate(R.layout.fragment_score_circular_landscape, container, false);
        } else {
            mMainView = inflater.inflate(R.layout.fragment_score_circular, container, false);
        }

        // initialize Views:
        stressScoreTextView = (TextView) findViewById(R.id.stressScoreTextView);
        durationTextView = (TextView) findViewById(R.id.durationTextView);
        durationSeekBar = (SeekBar) findViewById(R.id.durationSeekBar);

        durationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                updateNormClassifierOutput();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                translateProgress2Duration(seekBar);
            }
        });

        stressScoreTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateNormClassifierOutput();
            }
        });

        scoreAnimater = new ScoreTextAnimation();
        return mMainView;
    }

    protected void updateNormClassifierOutput() {
        // @TODO: this is just demo data, will replace with realworld data later
        Random rand = new Random();
        normClassifierOutput = (30 + rand.nextInt(70)) / 100.f;
        scoreAnimater.stopThread();
        circularAnimation().start();
        myThread = new Thread(scoreAnimater);
        myThread.start();
    }

    @Override
    public void onResume() {
        // kick off the data generating thread:
        super.onResume();
        ivDrawable = (ImageView) findViewById(R.id.iv_drawable);
        circularDrawable = new CircularProgressDrawable.Builder()
                .setRingWidth(getResources().getDimensionPixelSize(R.dimen.drawable_ring_size))
                .setOutlineColor(getResources().getColor(android.R.color.darker_gray))
                .setRingColor(getResources().getColor(android.R.color.holo_green_light))
//                .setCenterColor(getResources().getColor(android.R.color.holo_blue_dark))
                .create();
        ivDrawable.setImageDrawable(circularDrawable);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * circularAnimation will fill the outer ring while applying a color effect from red to green
     *
     * @return Animation
     */
    private Animator circularAnimation() {
        AnimatorSet animation = new AnimatorSet();

        ObjectAnimator progressAnimation = ObjectAnimator.ofFloat(circularDrawable, CircularProgressDrawable.PROGRESS_PROPERTY,
                0f, normClassifierOutput);
        progressAnimation.setDuration(ANIM_DURATION);
        progressAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator colorAnimator = setDynamicColorsArguement();
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setDuration(ANIM_DURATION);

        animation.playTogether(progressAnimation, colorAnimator);
        return animation;
    }

    private ObjectAnimator setDynamicColorsArguement() {
        ObjectAnimator colorAnimator;
        if (normClassifierOutput < 0.2) {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light));
        } else if (normClassifierOutput < 0.4) {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_blue_light));
        } else if (normClassifierOutput < 0.6) {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_blue_light),
                    getResources().getColor(android.R.color.holo_purple));
        } else if (normClassifierOutput < 0.8) {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_blue_light),
                    getResources().getColor(android.R.color.holo_purple),
                    getResources().getColor(android.R.color.holo_orange_light));
        } else {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_blue_light),
                    getResources().getColor(android.R.color.holo_purple),
                    getResources().getColor(android.R.color.holo_orange_light),
                    getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    protected class ScoreTextAnimation implements Runnable {
        // Circular animation
        private boolean keepRunning = false;

        public void stopThread() {
            keepRunning = false;
            dynamicPercentage = 0.f;
            updateScoreTextViewInUiThread();
        }

        //@Override
        public void run() {
            try {
                keepRunning = true;
                float step = SLEEP_INTER * normClassifierOutput / ANIM_DURATION;
                while(dynamicPercentage <= normClassifierOutput && keepRunning) {
                    Thread.sleep(SLEEP_INTER);
                    dynamicPercentage += step;
                    System.out.println(dynamicPercentage);
                    updateScoreTextViewInUiThread();    // This has to be done in UI Thread!!
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        protected void updateScoreTextViewInUiThread() {
            stressScoreTextView.post(new Runnable() {
                @Override
                public void run() {
                    stressScoreTextView.setText(String.format("%.0f", Math.floor(dynamicPercentage * 100)));
                }
            });
        }

    }

    private final View findViewById(int id) {
        return mMainView.findViewById(id);
    }

    protected void translateProgress2Duration(SeekBar seekBar) {
        final int progress = seekBar.getProgress();
        durationTextView.post(new Runnable() {
            @Override
            public void run() {
                String base = "past 1 ";
                switch (progress) {
                    case 1:
                        durationTextView.setText(base + "day");
                        break;
                    case 2:
                        durationTextView.setText(base + "week");
                        break;
                    case 3:
                        durationTextView.setText(base + "month");
                        break;
                    case 4:
                        durationTextView.setText(base + "season");
                        break;
                    case 5:
                        durationTextView.setText(base + "year");
                        break;
                    default:
                        durationTextView.setText("now");
                }
            }
        });
    }

}
