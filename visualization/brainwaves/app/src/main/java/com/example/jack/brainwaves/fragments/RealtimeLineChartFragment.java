
package com.example.jack.brainwaves.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jack.brainwaves.MainActivity;
import com.example.jack.brainwaves.R;
import com.example.jack.brainwaves.helper.OrientationHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Highlight;
import com.interaxon.libmuse.Accelerometer;
import com.interaxon.libmuse.ConnectionState;
import com.interaxon.libmuse.Eeg;
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
import com.kyleduo.switchbutton.SwitchButton;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class RealtimeLineChartFragment extends SuperAwesomeCardFragment implements
        OnChartValueSelectedListener {

    private Muse muse;
    private ConnectionListener connectionListener;
    private DataListener dataListener;
    private boolean dataTransmission = true;

    private LineChart mChart;
    private SwitchButton locksb;
    private boolean readyToPlot = false;
    private RealtimeDataSource demoDataSource;

    final static float DOT_SIZE = 0f;
    final static float LINE_SIZE = 1f;
    final static int ALPHA = 80;
    final static int VISIBLE_RANGE = 100;
    static final int SAMPLE_INTERVAL = 100;
    static final float YMAX = 1000f;

    private int xcnt = 0;

    public static RealtimeLineChartFragment newInstance(int position) {
        RealtimeLineChartFragment f = new RealtimeLineChartFragment();
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
        inflateLayout2Fragment(R.layout.fragment_realtime_linechart);

//        addentry = (Button) findViewById(R.id.addentry);
        locksb = (SwitchButton) findViewById(R.id.locksb);
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.setOnChartValueSelectedListener(this);

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
        mChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(false);

        // set an alternative background color
        mChart.setBackgroundColor(Color.WHITE);

        LineData data = new LineData();
//        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        Typeface tf = Typeface.createFromAsset(mMainActivity.getAssets(), "OpenSans-Regular.ttf");

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        // l.setPosition(LegendPosition.LEFT_OF_CHART);
        l.setForm(LegendForm.LINE);
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
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setAxisMaxValue(YMAX);
        leftAxis.setDrawGridLines(false);
        
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        demoDataSource = new RealtimeDataSource();

//        addentry.setOnClickListener(new Button.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                addDemoEntry();
//            }
//        });

        locksb.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        locksb.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                doRealtimePlot(b);
            }
        });

        // Create listeners and pass reference to activity to them
        WeakReference<Activity> weakActivity =
                new WeakReference<Activity>(mMainActivity);
        connectionListener = new ConnectionListener(weakActivity);
        dataListener = new DataListener(weakActivity);

        return mMainView;
    }

    private void doRealtimePlot(boolean b) {
        ((MainActivity) mMainActivity).onSettingPageLocker(b);
        boolean demoMode = ((MainActivity)mMainActivity).tryGetIsdemo();
        readyToPlot = b;
        demoDataSource.stopThread();
        if(b) {
            if(demoMode) {
                new Thread(demoDataSource).start();
            } else {
                connectMuse();
            }
        } else {
            if(!demoMode) {
                dataTransmission = !dataTransmission;
                if (muse != null) {
                    muse.enableDataTransmission(dataTransmission);
                }
            }
        }
    }

    private void connectMuse() {
        MuseManager.refreshPairedMuses();
        List<Muse> pairedMuses = MuseManager.getPairedMuses();
        if (pairedMuses.size() < 1) {
            Toast.makeText(mMainActivity, "Connect fails. Please go to MUSE CONFIG page to test.", Toast.LENGTH_LONG).show();
        }
        else {
            muse = pairedMuses.get(0);      // @TODO: now only connect to one device
            ConnectionState state = muse.getConnectionState();
            if (state == ConnectionState.CONNECTED ||
                    state == ConnectionState.CONNECTING) {
                return;
            }
            configure_library();
            /**
             * In most cases libmuse native library takes care about
             * exceptions and recovery mechanism, but native code still
             * may throw in some unexpected situations (like bad bluetooth
             * connection). Print all exceptions here.
             */
            try {
                muse.runAsynchronously();
            } catch (Exception e) {
                Log.e("Muse Headband", e.toString());
            }
        }
    }

    private void configure_library() {
        muse.registerConnectionListener(connectionListener);
        muse.registerDataListener(dataListener,
                MuseDataPacketType.ACCELEROMETER);
        muse.registerDataListener(dataListener,
                MuseDataPacketType.EEG);
        muse.registerDataListener(dataListener,
                MuseDataPacketType.ALPHA_RELATIVE);
        muse.registerDataListener(dataListener,
                MuseDataPacketType.ARTIFACTS);
        muse.registerDataListener(dataListener,
                MuseDataPacketType.HORSESHOE);
        muse.setPreset(MusePreset.PRESET_14);
        muse.enableDataTransmission(dataTransmission);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.actionAdd: {
                addDemoEntry();
                break;
            }
        }
        return true;
    }

    private void addDemoEntry() {

        LineData data = mChart.getData();

        if (data != null) {

            LineDataSet set = data.getDataSetByIndex(0);
            // set.addDemoEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            // add a new x-value first
            data.addXValue(String.valueOf(xcnt++));
            data.addEntry(new Entry((float) (Math.random() * 40) + 40f, set.getEntryCount()), 0);

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
             mChart.setVisibleXRange(VISIBLE_RANGE);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

             // move to the latest entry
             mChart.moveViewToX(data.getXValCount()-7);

             // this automatically refreshes the chart (calls invalidate())
//             mChart.moveViewTo(data.getXValCount()-7, 55f, AxisDependency.LEFT);

            // redraw the chart
//            mChart.invalidate();
        }
    }

    private void addEntry(final ArrayList<Double> museData) {


    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Alpha");
        set.setAxisDependency(AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(LINE_SIZE);
        set.setCircleSize(DOT_SIZE);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(0f);
        return set;
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        Log.i("Entry selected", e.toString());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }

    protected class RealtimeDataSource implements Runnable {
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
                while(keepRunning) {
                    mChart.post(new Runnable() {
                        @Override
                        public void run() {
                            addDemoEntry();
                        }
                    });
                    Thread.sleep(SAMPLE_INTERVAL);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Connection listener updates UI with new connection status and logs it.
     */
    public class ConnectionListener extends MuseConnectionListener {

        final WeakReference<Activity> activityRef;

        ConnectionListener(final WeakReference<Activity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(MuseConnectionPacket p) {
            final ConnectionState current = p.getCurrentConnectionState();
            final String status = p.getPreviousConnectionState().toString() +
                    " -> " + current;
            final String full = "Muse " + p.getSource().getMacAddress() +
                    " " + status;
            Log.i("Muse Headband", full);
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (current == ConnectionState.CONNECTED) {
                            Toast.makeText(mMainActivity, "Connected to MUSE", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mMainActivity, "Muse undefined...", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    /**
     * Data listener will be registered to listen for: Accelerometer,
     * Eeg and Relative Alpha bandpower packets. In all cases we will
     * update UI with new values.
     * We also will log message if Artifact packets contains "blink" flag.
     */
    public class DataListener extends MuseDataListener {

        final WeakReference<Activity> activityRef;
        private DataOutputStream eegOut;
        private DataOutputStream accelerometerOut;
        private DataOutputStream horseShoeOut;
        private DataOutputStream blinkOut;

        DataListener(final WeakReference<Activity> activityRef) {
            this.activityRef = activityRef;
        }

        private DataOutputStream createBinaryFile(File f) {
            DataOutputStream dataOutputStream = null;
            try {
                dataOutputStream = new DataOutputStream(new FileOutputStream(f));
                MediaScannerConnection.scanFile(activityRef.get(), new String[]{f.getAbsolutePath()}, null, null);
            }
            catch (IOException e) {
                System.exit(1);
                e.printStackTrace();
            }
            return dataOutputStream;
        }

        @Override
        public void receiveMuseDataPacket(MuseDataPacket p) {
            switch (p.getPacketType()) {
                case EEG:
                    updateEeg(p.getValues());
                    break;
                case ACCELEROMETER:
                    updateAccelerometer(p.getValues());
                    break;
                case HORSESHOE:
                    updateHorseshoe(p.getValues());
                    break;
                default:
                    break;
            }
        }

        @Override
        public void receiveMuseArtifactPacket(MuseArtifactPacket p) {
            if (p.getHeadbandOn() && p.getBlink()) {
                Log.i("Artifacts", "blink");
            }
        }

        long lastEEGUpdate, lastAccelerometerUpdate, lastHorseshoeUpdate;

        private void updateAccelerometer(final ArrayList<Double> data) {
            if (System.currentTimeMillis() - lastAccelerometerUpdate > 500) {
                lastAccelerometerUpdate = System.currentTimeMillis();
            }
            else {
                return;
            }
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        acc_x.setText(String.format(
//                                "%6.2f", data.get(Accelerometer.FORWARD_BACKWARD.ordinal())));
//                        acc_y.setText(String.format(
//                                "%6.2f", data.get(Accelerometer.UP_DOWN.ordinal())));
//                        acc_z.setText(String.format(
//                                "%6.2f", data.get(Accelerometer.LEFT_RIGHT.ordinal())));
                    }
                });
            }


        }

        private void updateEeg(final ArrayList<Double> museData) {

            if (System.currentTimeMillis() - lastEEGUpdate > 500) {
                lastEEGUpdate = System.currentTimeMillis();
            }
            else {
                return;
            }
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LineData data = mChart.getData();

                        float eegval = (float) (museData.get(Eeg.TP9.ordinal())
                                                        + museData.get(Eeg.FP1.ordinal())
                                                        + museData.get(Eeg.FP2.ordinal())
                                                        + museData.get(Eeg.TP10.ordinal()));
                        eegval /= 4;
                        if (data != null) {

                            LineDataSet set = data.getDataSetByIndex(0);
                            // set.addDemoEntry(...); // can be called as well

                            if (set == null) {
                                set = createSet();
                                data.addDataSet(set);
                            }

                            // add a new x-value first
                            data.addXValue(String.valueOf(xcnt++));
                            data.addEntry(new Entry(eegval, set.getEntryCount()), 0);
                            mChart.notifyDataSetChanged();
                            mChart.setVisibleXRange(VISIBLE_RANGE);
                            mChart.moveViewToX(data.getXValCount()-7);
                        }
                    }
                });
            }
        }

        private void updateHorseshoe(final ArrayList<Double> data) {
            if (System.currentTimeMillis() - lastHorseshoeUpdate > 500) {
                lastHorseshoeUpdate = System.currentTimeMillis();
            }
            else {
                return;
            }
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        elem1.setText(String.format(
//                                "L Ear %.1f", data.get(Eeg.TP9.ordinal())));
//                        elem2.setText(String.format(
//                                "L Front %.1f", data.get(Eeg.FP1.ordinal())));
//                        elem3.setText(String.format(
//                                "R Front %.1f", data.get(Eeg.FP2.ordinal())));
//                        elem4.setText(String.format(
//                                "R Ear %.1f", data.get(Eeg.TP10.ordinal())));
                    }
                });
            }
        }
    }

    // Container Activity must implement this interface
    public interface onRealtimeplotListener {
        public boolean tryGetIsdemo();
    }
}
