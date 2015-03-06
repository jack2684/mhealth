package com.example.jack.brainwaves.fragments;

import com.example.jack.brainwaves.MainActivity;
import com.example.jack.brainwaves.R;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.jack.brainwaves.helper.MyMarkerView;
import com.example.jack.brainwaves.helper.OrientationHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.renderer.ViewPortHandler;
import com.github.mikephil.charting.utils.Highlight;
import com.kyleduo.switchbutton.SwitchButton;

import java.util.ArrayList;

/**
 * Created by jack on 3/2/15.
 */
public class LogPlotFragment extends SuperAwesomeCardFragment implements SeekBar.OnSeekBarChangeListener,
        OnChartGestureListener, OnChartValueSelectedListener {
    final static int DATA_CNT_BASE = 3;
    final static int DATA_CNT_POWER = 3;
    private LineChart mChart;
    private SwitchButton locksb;
    private Button moreButton, lessButton;
    private TextView unitTextView;
    private int unitPicked;
    ViewPortHandler vpHandler;
    private String unitStrings[] = {
            "Daily ago",
            "Weekly ago",
            "Monthly ago",
            "Seasonly ago",
            "Yearly ago",
    };
    private LimitLine limitLine = new LimitLine(100f, "Upper Limit");

    final static int ANIM_DURATION = 1000;

    public static LogPlotFragment newInstance(int position) {
        LogPlotFragment f = new LogPlotFragment();
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
        inflateLayout2Fragment(R.layout.fragment_logplot);


        locksb = (SwitchButton) findViewById(R.id.locksb);
        mChart = (LineChart) findViewById(R.id.chart1);
        moreButton = (Button) findViewById(R.id.more);
        lessButton = (Button) findViewById(R.id.less);
        unitTextView = (TextView) findViewById(R.id.unit);
        mChart.setOnChartGestureListener(this);
        mChart.setOnChartValueSelectedListener(this);
        unitPicked = 2;
        unitTextView.setText(unitStrings[unitPicked]);

        // no description text
        mChart.setDescription("");
        mChart.setNoDataTextDescription("You need to provide data for the chart.");

        // enable value highlighting
        mChart.setHighlightEnabled(true);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        // mChart.setBackgroundColor(Color.GRAY);

        // create a custom MarkerView (extend MarkerView) and specify the layout
        // to use for it
        MyMarkerView mv = new MyMarkerView(mMainActivity, R.layout.component_marker_view);

        // set the marker to the chart
        mChart.setMarkerView(mv);

        // enable/disable highlight indicators (the lines that indicate the
        // highlighted Entry)
        mChart.setHighlightIndicatorEnabled(false);

        // add data
        setData((int) Math.pow(DATA_CNT_BASE, unitStrings.length - unitPicked), 0);
        mChart.animateY(ANIM_DURATION);

//        mChart.setVisibleYRange(30, AxisDependency.LEFT);

        mChart.zoom(10f, 1f, 50, 0);

        mChart.setPinchZoom(false);

        vpHandler = new ViewPortHandler();
        mChart.moveViewTo(0, 2.5f, YAxis.AxisDependency.LEFT);
//        mChart.centerViewPort(19, 19);


        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        // l.setPosition(LegendPosition.LEFT_OF_CHART);
        l.setForm(Legend.LegendForm.LINE);

        // // dont forget to refresh the drawing
        // mChart.invalidate();



        locksb.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        locksb.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                ((MainActivity) mMainActivity).onSettingPageLocker(b);
                if (b) {
                    setData((int) Math.pow(DATA_CNT_BASE, unitStrings.length - unitPicked), 5);
                }
            }
        });

        moreButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(unitPicked >= 0 && unitPicked < unitStrings.length - 1) {
                    unitPicked++;
                    unitTextView.setText(unitStrings[unitPicked]);
                    setData((int) Math.pow(DATA_CNT_BASE, unitStrings.length - unitPicked), 5);
                }
            }
        });

        lessButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (unitPicked >= 1 && unitPicked < unitStrings.length) {
                    unitPicked--;
                    unitTextView.setText(unitStrings[unitPicked]);
                    setData((int) Math.pow(DATA_CNT_BASE, unitStrings.length - unitPicked), 5);
                }
            }
        });

        return mMainView;
    }

    final static float DOT_SIZE = 6f;
    final static float LINE_SIZE = 1f;
    final static int ALPHA = 80;

    private void setData(int count, float range) {

        ArrayList<String> xVals = new ArrayList<String>();
        for (int i = count - 1; i >= 0; i--) {
            xVals.add((i) + "");
        }

        ArrayList<Entry> yVals = new ArrayList<Entry>();
        for (int i = 0; i < count; i++) {
            if(range == 0){
                float val = (float) (Math.random() * range * 0.8 + 1);// + (float)
                // ((mult *
                // 0.1) / 10);
                yVals.add(new Entry(val, i));
            } else {
                yVals.add(new Entry(0, i));
            }
        }

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(yVals, "MUSE");
        // set1.setFillAlpha(110);
        // set1.setFillColor(Color.RED);

        // set the line to be drawn like this "- - - - - -"
        set1.setColor(getResources().getColor(R.color.dark));
        set1.setCircleColor(getResources().getColor(R.color.dark));
        set1.setLineWidth(LINE_SIZE);
        set1.setCircleSize(DOT_SIZE);
        set1.setFillAlpha(ALPHA);
        set1.setFillColor(getResources().getColor(R.color.unhappy_blue));
        set1.setDrawFilled(true);
        set1.setDrawValues(false);




        ArrayList<Entry> yVals2 = new ArrayList<Entry>();
        for (int i = 0; i < count; i++) {
            float val = (float) (Math.min(5, (0.5 + Math.random()) * yVals.get(i).getVal()));// + (float)
            // ((mult *
            // 0.1) / 10);
            yVals2.add(new Entry(val, i));
        }

        // create a dataset and give it a type
        LineDataSet set2 = new LineDataSet(yVals2, "PSS");
        // set1.setFillAlpha(110);
        // set1.setFillColor(Color.RED);

        // set the line to be drawn like this "- - - - - -"
        set2.setColor(getResources().getColor(R.color.dark));
        set2.setCircleColor(getResources().getColor(R.color.dark));
        set2.setLineWidth(LINE_SIZE);
        set2.setCircleSize(DOT_SIZE);
        set2.setFillAlpha(ALPHA);
        set2.setFillColor(getResources().getColor(R.color.unhappy_green));
        set2.setDrawFilled(true);
        set2.setDrawValues(false);

        // set1.setShader(new LinearGradient(0, 0, 0, mChart.getHeight(),
        // Color.BLACK, Color.WHITE, Shader.TileMode.MIRROR));

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(set1); // add the datasets
        dataSets.add(set2); // add the datasets

        // create a data object with the datasets
        LineData data = new LineData(xVals, dataSets);
        data.setValueTextSize(10f);

        limitLine.setLineWidth(1f);
        limitLine.enableDashedLine(10f, 10f, 0f);
        limitLine.setLabelPosition(LimitLine.LimitLabelPosition.POS_RIGHT);
        limitLine.setTextSize(10f);

        YAxis leftAxis = mChart.getAxisLeft();
        XAxis xAxis = mChart.getXAxis();
//        leftAxis.addLimitLine(limitLine);     //@TODO might deprecate limit latter
        leftAxis.setAxisMaxValue(6f);
        leftAxis.setAxisMinValue(0f);
        leftAxis.setStartAtZero(false);
        leftAxis.setDrawGridLines(false);
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        mChart.getAxisRight().setEnabled(false);
        // set data
        mChart.setData(data);
        mChart.animateY(ANIM_DURATION);
    }



    public interface onLogPlotListener {
        public void onSettingPageLocker(boolean b);
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//        mChart.zoom((float)mSeekBarX.getProgress() / 5f, 1f, mSeekBarY.getProgress(), 0);

//        mSeekBarX.setProgress(5);

        // redraw
        mChart.invalidate();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
