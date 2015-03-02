package com.example.jack.brainwaves;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;

import java.util.Random;

public class PieChartActivity extends Activity {

    private boolean firstTime;

    private TextView stressScoreTextView;
    private TextView durationTextView;
    private PieChart pie;
    private static final int[] buttonids = {
            R.id.startLogXYPlotButton,
            R.id.startOrSensorExButton,
            R.id.museandpssbtn,
    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Button startLogXYPlotButton = (Button)findViewById(R.id.startLogXYPlotButton);
        startLogXYPlotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PieChartActivity.this, LogXYPlotActivity.class));
            }
        });

        Button startOrSensorExButton = (Button) findViewById(R.id.startOrSensorExButton);
        startOrSensorExButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PieChartActivity.this, RealtimePlotActivity.class));
            }
        });

        Button startMuseButton = (Button) findViewById(R.id.museandpssbtn);
        startMuseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PieChartActivity.this, MuseActivity.class));
            }
        });

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
                    updateButtonsColor();
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

    protected void updateButtonsColor() {
        for(int i : buttonids) {
            Button b = (Button) findViewById(i);
            b.setBackgroundColor(getDynamicColor());
        }
    }

    protected int getDynamicColor() {
        Color c = new Color();
        return c.rgb(
                Math.round(dynamicColorR * dynamicPercentage),
                dynamicColorG,
                dynamicColorB
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
