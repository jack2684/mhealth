package com.example.jack.brainwaves.fragments;

import android.app.Activity;
import android.content.DialogInterface;
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
public class MuseFragment extends SuperAwesomeCardFragment implements View.OnClickListener {
    private Muse muse;
    private ConnectionListener connectionListener;
    private DataListener dataListener;
    private boolean dataTransmission = true;
    boolean isSessionNameInputActive;

    public static MuseFragment newInstance(int position) {
        MuseFragment f = new MuseFragment();
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
        inflateLayout2Fragment(R.layout.fragment_muse, R.layout.fragment_muse_landscape);

        // Create listeners and pass reference to activity to them
        WeakReference<Activity> weakActivity =
                new WeakReference<Activity>(mMainActivity);
        connectionListener = new ConnectionListener(weakActivity);
        dataListener = new DataListener(weakActivity);
        Button refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);
        Button connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(this);
        Button disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(this);
        Button pauseButton = (Button) findViewById(R.id.pause);
        pauseButton.setOnClickListener(this);
        Log.i("Muse Headband", "libmuse version=" + LibMuseVersion.SDK_VERSION);
        setIsSessionNameInputActive(true);
        return mMainView;
    }

    @Override
    public void onDestroy() {
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
                    mMainActivity,
                    android.R.layout.simple_spinner_item,
                    spinnerItems
            );
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
    }

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
                System.exit(1);
                e.printStackTrace();
            }
            return dataOutputStream;
        }

        public void prepareOutputFiles() {
            setIsSessionNameInputActive(false);
            String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
            File outDir = new File(root + "/muse_" + ((EditText) findViewById(R.id.sessionName)).getText() + "_" + System.currentTimeMillis());
            outDir.mkdirs();

            eegOut = createBinaryFile(new File(outDir, "eeg"));
            accelerometerOut = createBinaryFile(new File(outDir, "accelerometer"));
            horseShoeOut = createBinaryFile(new File(outDir, "horseshoe"));
            blinkOut = createBinaryFile(new File(outDir, "blink"));
        }



        public void closeOutputFiles() {
            setIsSessionNameInputActive(true);
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
                writeOutput(MuseDataPacketType.ARTIFACTS, System.currentTimeMillis()*1000, null);
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

    private void setIsSessionNameInputActive(boolean b) {
        isSessionNameInputActive = b;
        EditText sessionNameEditor = (EditText) findViewById(R.id.sessionName);
        sessionNameEditor.setEnabled(b);
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


}
