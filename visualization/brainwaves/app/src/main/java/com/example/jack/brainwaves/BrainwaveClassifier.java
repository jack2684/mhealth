package com.example.jack.brainwaves;

import android.os.Environment;
import android.support.v4.util.CircularArray;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

public class BrainwaveClassifier {
    private static final double SQRT_2 = Math.sqrt(2);
    private static CircularArray<ArrayList<Double> > eeg = new CircularArray<ArrayList<Double>>(256);
    private static double featureBuffer[] = new double[128*4];
    private static JSONObject jsonData;
    private static int numOfTrainingSamples;

    public static void init() {
        ArrayList<Double> zeros = new ArrayList<Double>();
        for (int i = 0; i < 4; i++) zeros.add((double) 0);
        for (int i = 0; i < 256; i++) {
            eeg.addLast(zeros);
        }

        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File model_json_file = new File(root + "/brainwave_model.json");
        String jsonStr = "";
        try {
            FileInputStream inputStream = new FileInputStream(model_json_file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
            jsonStr = sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            jsonData = new JSONObject(jsonStr);
            numOfTrainingSamples = jsonData.getJSONArray("train_labels").length();
//            Log.d("json", "" + jsonData.getJSONArray("train_data").getJSONArray(0).getDouble(0));
            Log.d("json", "" + numOfTrainingSamples);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void storeEEGData(final ArrayList<Double> data) {
        eeg.addLast(data);
        eeg.popFirst();
    }

    public static int getKnnLabel() {
        extractFeatures();
        PriorityQueue<Pair<Integer, Double>> priorityQueue = new PriorityQueue<Pair<Integer, Double>>(5,
            new Comparator<Pair<Integer, Double>>() {
                @Override
                public int compare(Pair<Integer, Double> e1, Pair<Integer, Double> e2) {
                    if (e1.second == e2.second) return 0;
                    else return e1.second < e2.second ? -1 : 1;
                }
            }
        );

        try {
            JSONArray train_data = jsonData.getJSONArray("train_data");
            JSONArray train_labels = jsonData.getJSONArray("train_labels");
            for (int i = 0; i < numOfTrainingSamples; i++) {
                priorityQueue.offer(new Pair<Integer, Double>(
                    train_labels.getInt(i), distanceFromTrainingVector(train_data.getJSONArray(i))
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        int pred_label = 0;
        for (int i = 0; i < 5; i++) {
            pred_label += priorityQueue.poll().first;
        }
        if (pred_label > 0) pred_label = 1;
        else pred_label = -1;
        return pred_label;
    }

    private static double distanceFromTrainingVector(JSONArray vec) {
        double dist = 0;
        for (int i = 0; i < featureBuffer.length; i++) {
            try {
                dist += Math.pow(vec.getDouble(i) - featureBuffer[i], 2);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return dist;
    }

    private static void extractFeatures() {
        double approx[] = new double[256];
        double detail[] = new double[256];
        for (int channel = 0; channel < 4; channel++) {
            // copy data to approx
            for (int i = 0; i < eeg.size(); i++) {
                approx[i] = eeg.get(i).get(channel);
            }
            // run DWT
            haar(approx, detail);
            // copy select coefficients to feature buffer
            featureBuffer[channel*128] = approx[1];
            for (int j = 1; j < 128; j++) {
                featureBuffer[channel*128 + j] = detail[j];
            }
        }
    }

    public static void runHaar() {
        double approx[] = {4, 5, 3, 7, 4, 5, 9,7, 2, 3, 3, 5, 0, 0, 0, 0};
        double detail[] = new double[approx.length];
        haar(approx, detail);
        Log.d("haar", "" + approx[1]);
        for (int i = 1; i < detail.length; i++) {
            Log.d("haar", "" + detail[i]);
        }
    }

    private static void haar(double [] approx, double[] detail) {
        // apply filter cascades in dyadic steps
        // fill in detail and approximation coefficients in the correct order
        int items_written = 0;
        for (int i = approx.length; i > 1; i /= 2) {
            int input_offset = items_written == 0 ? 0 : approx.length - items_written;
            // high pass
            for (int j = i - 1; j >= 0; j -= 2) {
                detail[approx.length - items_written - i / 2 + j / 2] =
                    (approx[input_offset + j - 1] - approx[input_offset + j]) / SQRT_2;
            }
            // low pass
            for (int j = i - 1; j >= 0; j -= 2) {
                approx[approx.length - items_written - i / 2 + j / 2] =
                    (approx[input_offset + j] + approx[input_offset + j - 1]) / SQRT_2;
            }
            items_written += i / 2;
        }
    }
}
