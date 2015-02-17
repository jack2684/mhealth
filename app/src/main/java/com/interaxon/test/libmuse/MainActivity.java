/**
 * Example of using libmuse library on android.
 * Interaxon, Inc. 2015
 */

package com.interaxon.test.libmuse;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

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


/**
 * In this simple example MainActivity implements 2 MuseHeadband listeners
 * and updates UI when data from Muse is received. Similarly you can implement
 * listers for other data or register same listener to listen for different type
 * of data.
 * For simplicity we create Listeners as inner classes of MainActivity. We pass
 * reference to MainActivity as we want listeners to update UI thread in this
 * example app.
 * You can also connect multiple muses to the same phone and register same
 * listener to listen for data from different muses. In this case you will
 * have to provide synchronization for data members you are using inside
 * your listener.
 *
 * Usage instructions:
 * 1. Enable bluetooth on your device
 * 2. Pair your device with muse
 * 3. Run this project
 * 4. Press Refresh. It should display all paired Muses in Spinner
 * 5. Make sure Muse headband is waiting for connection and press connect.
 * It may take up to 10 sec in some cases.
 * 6. You should see EEG and accelerometer data as well as connection status,
 * Version information and MuseElements (alpha, beta, theta, delta, gamma waves)
 * on the screen.
 */
public class MainActivity extends Activity implements OnClickListener {
    /**
     * Connection listener updates UI with new connection status and logs it.
     */
    class ConnectionListener extends MuseConnectionListener {

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
                        TextView statusText =
                                (TextView) findViewById(R.id.con_status);
                        statusText.setText(status);
                        TextView museVersionText =
                                (TextView) findViewById(R.id.version);
                        if (current == ConnectionState.CONNECTED) {
                            MuseVersion museVersion = muse.getMuseVersion();
                            String version = museVersion.getFirmwareType() +
                                 " - " + museVersion.getFirmwareVersion() +
                                 " - " + Integer.toString(
                                    museVersion.getProtocolVersion());
                            museVersionText.setText(version);
                        } else {
                            museVersionText.setText(R.string.undefined);
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
    class DataListener extends MuseDataListener {

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
                e.printStackTrace();
            }
            return dataOutputStream;
        }

        public void prepareOutputFiles() {
            String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
            File outDir = new File(root + "/muse_data_" + System.currentTimeMillis());
            outDir.mkdirs();

            eegOut = createBinaryFile(new File(outDir, "eeg"));
            accelerometerOut = createBinaryFile(new File(outDir, "accelerometer"));
            horseShoeOut = createBinaryFile(new File(outDir, "horseshoe"));
            blinkOut = createBinaryFile(new File(outDir, "blink"));
        }



        public void closeOutputFiles() {
            if (eegOut == null) return;
            try {
                eegOut.close();
                eegOut = null;

                accelerometerOut.close();
                accelerometerOut = null;

                horseShoeOut.close();
                horseShoeOut = null;

                blinkOut.close();
                blinkOut = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void writeOutput(MuseDataPacketType packetType, long timestamp, final ArrayList<Double> data) {
            DataOutputStream out = null;
            switch (packetType) {
                case EEG:
                    out = eegOut;
                    break;
                case ACCELEROMETER:
                    out = accelerometerOut;
                    break;
                case HORSESHOE:
                    out = horseShoeOut;
                    break;
                case ARTIFACTS:
                    out = blinkOut;
                    break;
                default:
                    break;
            }

            try {
                out.writeLong(timestamp);
                if (data != null) {
                    for (double d : data) {
                        out.writeDouble(d);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void receiveMuseDataPacket(MuseDataPacket p) {
            switch (p.getPacketType()) {
                case EEG:
                    writeOutput(MuseDataPacketType.EEG, p.getTimestamp(), p.getValues());
                    updateEeg(p.getValues());
                    break;
                case ACCELEROMETER:
                    writeOutput(MuseDataPacketType.ACCELEROMETER, p.getTimestamp(), p.getValues());
                    updateAccelerometer(p.getValues());
                    break;
                case HORSESHOE:
                    writeOutput(MuseDataPacketType.HORSESHOE, p.getTimestamp(), p.getValues());
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
                writeOutput(MuseDataPacketType.ARTIFACTS, System.currentTimeMillis(), null);
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
                        TextView acc_x = (TextView) findViewById(R.id.acc_x);
                        TextView acc_y = (TextView) findViewById(R.id.acc_y);
                        TextView acc_z = (TextView) findViewById(R.id.acc_z);
                        acc_x.setText(String.format(
                            "%6.2f", data.get(Accelerometer.FORWARD_BACKWARD.ordinal())));
                        acc_y.setText(String.format(
                            "%6.2f", data.get(Accelerometer.UP_DOWN.ordinal())));
                        acc_z.setText(String.format(
                            "%6.2f", data.get(Accelerometer.LEFT_RIGHT.ordinal())));
                    }
                });
            }
        }

        private void updateEeg(final ArrayList<Double> data) {
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
                         TextView tp9 = (TextView) findViewById(R.id.eeg_tp9);
                         TextView fp1 = (TextView) findViewById(R.id.eeg_fp1);
                         TextView fp2 = (TextView) findViewById(R.id.eeg_fp2);
                         TextView tp10 = (TextView) findViewById(R.id.eeg_tp10);
                         tp9.setText(String.format(
                            "%6.2f", data.get(Eeg.TP9.ordinal())));
                         fp1.setText(String.format(
                            "%6.2f", data.get(Eeg.FP1.ordinal())));
                         fp2.setText(String.format(
                            "%6.2f", data.get(Eeg.FP2.ordinal())));
                         tp10.setText(String.format(
                            "%6.2f", data.get(Eeg.TP10.ordinal())));
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
                         TextView elem1 = (TextView) findViewById(R.id.elem1);
                         TextView elem2 = (TextView) findViewById(R.id.elem2);
                         TextView elem3 = (TextView) findViewById(R.id.elem3);
                         TextView elem4 = (TextView) findViewById(R.id.elem4);
                         elem1.setText(String.format(
                            "L Ear %.1f", data.get(Eeg.TP9.ordinal())));
                         elem2.setText(String.format(
                            "L Front %.1f", data.get(Eeg.FP1.ordinal())));
                         elem3.setText(String.format(
                            "R Front %.1f", data.get(Eeg.FP2.ordinal())));
                         elem4.setText(String.format(
                            "R Ear %.1f", data.get(Eeg.TP10.ordinal())));
                    }
                });
            }
        }
    }


    private Muse muse;
    private ConnectionListener connectionListener;
    private DataListener dataListener;
    private boolean dataTransmission = true;

    private View perceivedStressScaleView;
    private View dataView;

    private TextView pssText;
    private RadioGroup pssRadioGroup;
    private int currentPssTextIndex;
    private String pssResponses = "";

    public MainActivity() {
        // Create listeners and pass reference to activity to them
        WeakReference<Activity> weakActivity =
                                new WeakReference<Activity>(this);
        connectionListener = new ConnectionListener(weakActivity);
        dataListener = new DataListener(weakActivity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        perceivedStressScaleView = ((LayoutInflater)
            getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.perceived_stress_scale, null
        );
        dataView = ((LayoutInflater)
            getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.activity_main, null
        );

        setContentView(R.layout.root_layout);
        ((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content)).addView(dataView);
        ((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content)).addView(perceivedStressScaleView);
        dataView.setVisibility(View.VISIBLE);
        perceivedStressScaleView.setVisibility(View.GONE);

        Button refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);
        Button connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(this);
        Button disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(this);
        Button pauseButton = (Button) findViewById(R.id.pause);
        pauseButton.setOnClickListener(this);

        Button button = (Button) findViewById(R.id.pss);
        button.setOnClickListener(this);

        button = (Button) findViewById(R.id.pss_close);
        button.setOnClickListener(this);

        button = (Button) findViewById(R.id.pss_next);
        button.setOnClickListener(this);

        pssText = (TextView) findViewById(R.id.pss_text);
        pssText.setText(PSSText[currentPssTextIndex]);

        pssRadioGroup = (RadioGroup) findViewById(R.id.pss_radio_group);
        ((RadioButton) findViewById(R.id.radio_never)).setChecked(true);

        Log.i("Muse Headband", "libmuse version=" + LibMuseVersion.SDK_VERSION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataListener.closeOutputFiles();
    }

    @Override
    public void onClick(View v) {
        Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);
        if (v.getId() == R.id.refresh) {
            MuseManager.refreshPairedMuses();
            List<Muse> pairedMuses = MuseManager.getPairedMuses();
            List<String> spinnerItems = new ArrayList<String>();
            for (Muse m: pairedMuses) {
                String dev_id = m.getName() + "-" + m.getMacAddress();
                Log.i("Muse Headband", dev_id);
                spinnerItems.add(dev_id);
            }
            ArrayAdapter<String> adapterArray = new ArrayAdapter<String> (
                    this, android.R.layout.simple_spinner_item, spinnerItems);
            musesSpinner.setAdapter(adapterArray);
        }
        else if (v.getId() == R.id.connect) {
            List<Muse> pairedMuses = MuseManager.getPairedMuses();
            if (pairedMuses.size() < 1 ||
                musesSpinner.getAdapter().getCount() < 1) {
                Log.w("Muse Headband", "There is nothing to connect to");
            }
            else {
                muse = pairedMuses.get(musesSpinner.getSelectedItemPosition());
                ConnectionState state = muse.getConnectionState();
                if (state == ConnectionState.CONNECTED ||
                    state == ConnectionState.CONNECTING) {
                    Log.w("Muse Headband", "doesn't make sense to connect second time to the same muse");
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
                    dataListener.prepareOutputFiles();
                } catch (Exception e) {
                    Log.e("Muse Headband", e.toString());
                }
            }
        }
        else if (v.getId() == R.id.disconnect) {
            if (muse != null) {
                muse.disconnect(true);
                dataListener.closeOutputFiles();
            }
        }
        else if (v.getId() == R.id.pause) {
            dataTransmission = !dataTransmission;
            if (muse != null) {
                muse.enableDataTransmission(dataTransmission);
            }
        }
        else if (v.getId() == R.id.pss) {
            dataView.setVisibility(View.GONE);
            perceivedStressScaleView.setVisibility(View.VISIBLE);
        }
        else if (v.getId() == R.id.pss_close) {
            perceivedStressScaleView.setVisibility(View.GONE);
            dataView.setVisibility(View.VISIBLE);
        }
        else if (v.getId() == R.id.pss_next) {

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
                perceivedStressScaleView.setVisibility(View.GONE);
                dataView.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Writing PSS Responses", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void writePssResponses() {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File outFile = new File(root + "/pss_responses_" + System.currentTimeMillis());
        try {
            outFile.createNewFile();
            FileOutputStream writer = new FileOutputStream(outFile);

            writer.write(pssResponses.getBytes());
            writer.flush();

            writer.close();
            MediaScannerConnection.scanFile(this, new String[]{outFile.getAbsolutePath()}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configure_library() {
        muse.registerConnectionListener(connectionListener);
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.ACCELEROMETER);
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.EEG);
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.ARTIFACTS);
        muse.registerDataListener(dataListener,
                                  MuseDataPacketType.HORSESHOE);
        muse.setPreset(MusePreset.PRESET_14);
        muse.enableDataTransmission(dataTransmission);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
