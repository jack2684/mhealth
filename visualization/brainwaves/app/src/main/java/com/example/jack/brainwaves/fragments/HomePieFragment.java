package com.example.jack.brainwaves.fragments;

import android.content.SyncAdapterType;
import android.support.v4.app.Fragment;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.example.jack.brainwaves.R;

import java.util.Random;

/**
 * Created by jack on 3/2/15.
 */
public class HomePieFragment extends Fragment {

    protected View mMainView;
    protected Activity mMainActivity;
    private static final String ARG_POSITION = "position";
    private int position;


    private boolean firstTime;

    private TextView stressScoreTextView;
    private TextView durationTextView;
    private PieChart pie;

    private Segment s1;
    private Segment s2;
    SegmentFormatter sf1 = new SegmentFormatter();
    SegmentFormatter sf2 = new SegmentFormatter();

    private float goldenPercentageOutput;
    private float dynamicPercentage;

    private Thread myThread;

    private DynamicScaleShow data;

    int dynamicColorR = 255;
    int dynamicColorG = 100;
    int dynamicColorB = 40;

    public static HomePieFragment newInstance(int position) {
        HomePieFragment f = new HomePieFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt(ARG_POSITION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainView =  inflater.inflate(R.layout.activity_pie_chart, container, false);
        mMainActivity = getActivity();

        SeekBar durationBar = (SeekBar) findViewById(R.id.durationSeekBar);
        durationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                redrawData();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                // TODO Auto-generated method stub
                translateProgress2Duration(seekBar);
            }
        });

        TextView startRefeshAnimation = (TextView) findViewById(R.id.donutSizeTextView);
        startRefeshAnimation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redrawData();
            }
        });

        // initialize Views:
        pie = (PieChart) findViewById(R.id.mySimplePieChart);
        stressScoreTextView = (TextView) findViewById(R.id.donutSizeTextView);
        durationTextView = (TextView) findViewById(R.id.durationTextView);
        updateDonutText();
        updateDonutColor();

        s1 = new Segment("", 20);
        s2 = new Segment("", 50);

        sf1.configure(getApplicationContext(), R.xml.pie_segment_formatter1);
        sf1.getFillPaint().setColor(getDynamicColor());
        sf2.configure(getApplicationContext(), R.xml.pie_segment_formatter2);

        pie.setPlotMarginBottom(0);
        pie.addSegment(s1, sf1);
        pie.addSegment(s2, sf2);
        pie.getBorderPaint().setColor(Color.TRANSPARENT);
        pie.getBackgroundPaint().setColor(Color.TRANSPARENT);
        pie.getRenderer(PieRenderer.class).setDonutSize(.85f, PieRenderer.DonutMode.PERCENT);
        pie.redraw();

        data = new DynamicScaleShow();
        firstTime = true;

        return mMainView;
    }

    private final View findViewById(int id) {
        return mMainView.findViewById(id);
    }

    private Context getApplicationContext() {
        return mMainActivity.getApplicationContext();
    }

    @Override
    public void onResume() {
        // kick off the data generating thread:
        if(firstTime) {
            redrawData();
            firstTime = false;
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        data.stopThread();
        super.onPause();
    }

    class DynamicScaleShow extends Activity implements Runnable {

        private boolean keepRunning = false;

        public void stopThread() {
            keepRunning = false;
        }

        //@Override
        public void run() {
            try {
                keepRunning = true;
                s1.setValue(0);
                dynamicPercentage = .0f;
//                float diff = Math.abs(dynamicPercentage - goldenPercentageOutput);
                int interval = 20;
                int animat_duration = 3000;
                float step = interval * goldenPercentageOutput / animat_duration;
                while(dynamicPercentage <= goldenPercentageOutput && keepRunning) {
                    Thread.sleep(interval);
                    s1.setValue(s2.getValue().floatValue() * dynamicPercentage / (1 - dynamicPercentage));
                    dynamicPercentage += step;
                    updatePie();
                    updateUIInUIThread();    // This has to be done in UI Thread!!
                }
                System.out.println(this.getClass().getName() + ": Animation down");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        protected void updateUIInUIThread() {
            stressScoreTextView.post(new Runnable() {
                @Override
                public void run() {
                    updateDonutText();
                    updateDonutColor();
                }
            });
        }
    }

    protected void redrawData() {
        // @TODO: this is just demo data, will replace with realworld data later
        Random rand = new Random();
        goldenPercentageOutput = (60 + rand.nextInt(40)) / 100.f;
        data.stopThread();
        myThread = new Thread(data);
        myThread.start();
    }

    protected void translateProgress2Duration(SeekBar seekBar) {
        final int progress = seekBar.getProgress();
        durationTextView.post(new Runnable() {
            @Override
            public void run() {
                String base = "For the past one ";
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
                        durationTextView.setText("For now");
                }
            }
        });
    }

    protected void updatePie() {
        sf1.getFillPaint().setColor(getDynamicColor());
        pie.redraw();
    }

    protected void updateDonutText() {
        stressScoreTextView.setText(String.format("%.0f", dynamicPercentage * 100));
    }

    protected void updateDonutColor() {
        stressScoreTextView.setTextColor(getDynamicColor());
    }

    protected int getDynamicColor() {
        Color c = new Color();
        return c.rgb(
                Math.round(dynamicColorR * dynamicPercentage),
                dynamicColorG,
                dynamicColorB
        );
    }
}
