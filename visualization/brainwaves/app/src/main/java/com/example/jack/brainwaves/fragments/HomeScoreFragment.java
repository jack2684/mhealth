package com.example.jack.brainwaves.fragments;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.interaxon.libmuse.Eeg;
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
    static final int LINE_COLOR = 0;
    final static float DOT_SIZE = 3f;
    final static float LINE_SIZE = 1f;
    final static int ALPHA = 80;
    final static int VISIBLE_RANGE = 50;
    static final int SAMPLE_INTERVAL = 500;
    static final float YMAX = 1.5f;

    // Views
    private TextView stressScoreTextView, durationTextView, moreButton, lessButton, hintTextView;
    private ImageView ivDrawable;
    private RangeBar timeRangeber;
    private LineChart mChart;
    private CircularProgressDrawable circularDrawable;

    // Key to the show
    private float normClassifierOutput;     // Normalized, expected to be 0 to 1
    private Animator circularAnimater;
    private ScoreTextAnimationHelper scoreAnimator;
    private Thread myThread;
    private boolean twoClassificationMode = false;
    private int xcnt = 0;

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
        this.inflater = inflater;
        this.container = container;
        mMainActivity = getActivity();
        isLandscape = OrientationHelper.isLandsacpe(mMainActivity);
        inflateLayout2Fragment(R.layout.fragment_home, R.layout.fragment_home_landscape);

        // initialize Views:
        stressScoreTextView = (TextView) findViewById(R.id.stressScoreTextView);
        durationTextView = (TextView) findViewById(R.id.durationTextView);
        ivDrawable = (ImageView) findViewById(R.id.iv_drawable);
        moreButton = (TextView) findViewById(R.id.more);
        lessButton = (TextView) findViewById(R.id.less);
        hintTextView = (TextView) findViewById(R.id.hint);
        timeRangeber = (RangeBar) findViewById(R.id.materialBar);
        timeRangeber.setSelectorColor(getResources().getColor(android.R.color.darker_gray));
        timeRangeber.setConnectingLineColor(getResources().getColor(android.R.color.darker_gray));
        timeRangeber.setPinColor(getResources().getColor(android.R.color.darker_gray));
        timeRangeber.setSeekPinByIndex(2);
        timeRangeber.setPinRadius(30f);
        translateProgress2Duration();
        configChart();

        // Setup circular animation
        scoreAnimator = new ScoreTextAnimationHelper();
        circularDrawable = new CircularProgressDrawable.Builder()
                .setRingWidth(getResources().getDimensionPixelSize(R.dimen.drawable_ring_size))
                .setOutlineColor(getResources().getColor(android.R.color.darker_gray))
                .setRingColor(getResources().getColor(android.R.color.holo_green_light))
                .create();
        ivDrawable.setImageDrawable(circularDrawable);

        // Setup Listeners
        timeRangeber.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
                                              int rightPinIndex,
                                              String leftPinValue, String rightPinValue) {
                updateTimeRange();
            }
        });

        stressScoreTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateNormClassifierOutput();
            }
        });

        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeRangeber.setSeekPinByIndex(Math.min(timeRangeber.getRightIndex() + 1, timeRangeber.getTickCount()));
                updateNormClassifierOutput();
            }
        });

        lessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeRangeber.setSeekPinByIndex(Math.max(timeRangeber.getRightIndex() - 1, 0));
                updateNormClassifierOutput();
            }
        });

        return mMainView;
    }

    private void configChart() {
        // Set mchart
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(false);
        mChart.setBackgroundColor(Color.TRANSPARENT);
        mChart.setData(new LineData());
        mChart.setVisibility(View.GONE);
        mChart.getData().addDataSet(createSet());
        mChart.setDescription("");
        mChart.setDescriptionTextSize(0f);

        // Set legend
        Typeface tf = Typeface.createFromAsset(mMainActivity.getAssets(), "OpenSans-Regular.ttf");
        Legend l = mChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTypeface(tf);
        l.setTextColor(Color.DKGRAY);

        XAxis xl = mChart.getXAxis();
        xl.setTypeface(tf);
        xl.setTextColor(Color.DKGRAY);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setPosition(XAxis.XAxisPosition.TOP_INSIDE);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTypeface(tf);
        leftAxis.setTextColor(Color.TRANSPARENT);
        leftAxis.setAxisMaxValue(YMAX);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisLineColor(Color.TRANSPARENT);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    protected void updateTimeRange() {
        scoreAnimator.stopThread();
        clearAnimation();
        translateProgress2Duration();
        updateScoreTextViewInUiThread("TAP");
    }

    protected void updateNormClassifierOutput() {
        // @TODO: this is just demo data, will replace with realworld data later
        if(timeRangeber.getRightIndex() != 0) {
            int ridx = timeRangeber.getRightIndex();
            int cnt = timeRangeber.getTickCount();
            normClassifierOutput = (float) (.9 * ((ridx + 3) % cnt) / cnt + 0.1f);
            if (ridx == 0) {
                Random rand = new Random();
                normClassifierOutput = rand.nextInt(100) / 100f;
            }
            scoreAnimator.stopThread();
            circularAnimation();
            myThread = new Thread(scoreAnimator);
            myThread.start();
        }
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
                    getResources().getColor(android.R.color.holo_orange_dark));
        } else if (normClassifierOutput < 1.1) {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_blue_light),
                    getResources().getColor(android.R.color.holo_purple),
                    getResources().getColor(android.R.color.holo_orange_dark),
                    getResources().getColor(android.R.color.holo_red_dark));
        } else {
            return ObjectAnimator.ofInt(circularDrawable, CircularProgressDrawable.RING_COLOR_PROPERTY,
                    getResources().getColor(android.R.color.black)
                    );
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
                keepRunning = true;
                digit = 0.f;
                float step = SLEEP_INTER * normClassifierOutput / ANIM_DURATION;
                while(digit <= normClassifierOutput && keepRunning) {
                    Thread.sleep(SLEEP_INTER);
                    digit += step;
                    updateScoreTextViewInUiThread(
                            String.format("%.1f", digit * 5));    // This has to be done in UI Thread!!
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
        final int progress = timeRangeber.getRightIndex();
        durationTextView.post(new Runnable() {
            @Override
            public void run() {
                String base = "past";
                stressScoreTextView.setVisibility(View.VISIBLE);
                hintTextView.setVisibility(View.VISIBLE);
                ivDrawable.setVisibility(View.VISIBLE);
                mChart.setVisibility(View.GONE);
                twoClassificationMode = false;
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
                    case 0:
                        durationTextView.setText("now");
                        stressScoreTextView.setVisibility(View.GONE);
                        hintTextView.setVisibility(View.GONE);
                        ivDrawable.setVisibility(View.GONE);
                        mChart.setVisibility(View.VISIBLE);
                        twoClassificationMode = true;
                        break;
                    default:
                }
            }
        });
    }

    public void plotTwoClassification(final boolean stressful) {
        if(twoClassificationMode) {
            mMainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LineData data = mChart.getData();
                    if (data != null) {
                        LineDataSet set = data.getDataSetByIndex(0);
                        if (set == null) {
                            System.err.println("Cannot find dataset!");
                        }
                        data.addXValue(String.valueOf(xcnt++));
                        data.addEntry(new Entry(stressful ? 1 : 0, set.getEntryCount()), 0);
                        mChart.notifyDataSetChanged();
                        mChart.setVisibleXRange(VISIBLE_RANGE);
                        mChart.moveViewToX(data.getXValCount() - 7);
                        Log.i("JAAAAAAAAAAAAAAAAAACK: ", String.valueOf(stressful));
                    }
                }
            });
        }
    }


    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Stressful OR Not?");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.LTGRAY);
        set.setCircleColor(ColorTemplate.JOYFUL_COLORS[LINE_COLOR]);
        set.setLineWidth(LINE_SIZE);
        set.setCircleSize(DOT_SIZE);
        set.setFillAlpha(ALPHA);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(0f);
        return set;
    }

    // Container Activity must implement this interface
    public interface onScoreListener {
        public int[] tryGetPreviousState();     // Score and
    }
}
