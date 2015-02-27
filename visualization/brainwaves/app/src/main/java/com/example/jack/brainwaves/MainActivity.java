package com.example.jack.brainwaves;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
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

public class MainActivity extends Activity {

    public final static String EXTRA_MESSAGE = "com.mycompany.myfirstapp.MESSAGE";

    private TextView donutSizeTextView;
    private SeekBar donutSizeSeekBar;

    private PieChart pie;

    private Segment s1;
    private Segment s2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startDynamicXYExButton = (Button)findViewById(R.id.startDynamicXYExButton);
        startDynamicXYExButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, DynamicXYPlotActivity.class));
            }
        });

        Button startOrSensorExButton = (Button) findViewById(R.id.startOrSensorExButton);
        startOrSensorExButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, OrientationSensorExampleActivity.class));
            }
        });

        Button startMuseButton = (Button) findViewById(R.id.museandpssbtn);
        startMuseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, MuseActivity.class));
            }
        });

        // initialize our XYPlot reference:
        pie = (PieChart) findViewById(R.id.mySimplePieChart);


        donutSizeSeekBar = (SeekBar) findViewById(R.id.donutSizeSeekBar);

        donutSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                s1.setValue((s1.getValue().intValue() + 1));
                pie.redraw();
                updateDonutText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        donutSizeTextView = (TextView) findViewById(R.id.donutSizeTextView);
        updateDonutText();

        s1 = new Segment("", 20);
        s2 = new Segment("", 10);

        EmbossMaskFilter emf = new EmbossMaskFilter(
                new float[]{1, 1, 1}, 0.4f, 10, 8.2f);

        SegmentFormatter sf1 = new SegmentFormatter();
        sf1.configure(getApplicationContext(), R.xml.pie_segment_formatter1);
        sf1.getFillPaint().setMaskFilter(emf);

        SegmentFormatter sf2 = new SegmentFormatter();
        sf2.configure(getApplicationContext(), R.xml.pie_segment_formatter2);
        sf2.getFillPaint().setMaskFilter(emf);

        pie.setPlotMarginBottom(0);
        pie.addSegment(s1, sf1);
        pie.addSegment(s2, sf2);
        pie.redraw();
        pie.getBorderPaint().setColor(Color.TRANSPARENT);
        pie.getBackgroundPaint().setColor(Color.TRANSPARENT);

        pie.getRenderer(PieRenderer.class).setDonutSize(.80f, PieRenderer.DonutMode.PERCENT);
    }

    protected void updateDonutText() {
        donutSizeTextView.setText(donutSizeSeekBar.getProgress() + "%");
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
