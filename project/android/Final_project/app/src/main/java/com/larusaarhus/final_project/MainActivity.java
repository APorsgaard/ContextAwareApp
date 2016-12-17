package com.larusaarhus.final_project;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String LOG_TAG = "FILE_WRITER";
    private int counter = 0;
    private boolean first = true;
    private ArrayList<Position> buffer1, buffer2;
    private int fileIncrementer;
    private AsyncTask myAsyncTask;
    private AsyncTask calcTask;
    private boolean start;
    private SensorManager sensorManager;
    private ArrayList<String> data;
    private ArrayList<Lable> lables;
    private Lable lable = Lable.HAND;
    private TextView textView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
        buffer1 = new ArrayList<>();
        buffer2 = new ArrayList<>();
        data = new ArrayList<>();
        lables = new ArrayList<>();
        start = false;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        fileIncrementer = sharedPref.getInt("incrementor", 0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStop() {
        start = true;
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        start = true;
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        start = true;
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accel,
                SensorManager.SENSOR_DELAY_FASTEST);
    }


    /* ---- SensorEvent ---- */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (start) {
            long timeInMillis = (new Date()).getTime()
                    + (sensorEvent.timestamp - System.nanoTime()) / 1000000L;
            float[] values = sensorEvent.values;
            if (first) {
                if (counter < 64) {
                    buffer1.add(new Position(timeInMillis, values[0], values[1], values[2]));
                    counter++;
                } else {
                    first = false;
                    counter = 0;
                }
            } else if (counter < 64) {
                buffer1.add(new Position(timeInMillis, values[0], values[1], values[2]));
                buffer2.add(new Position(timeInMillis, values[0], values[1], values[2]));
                counter++;
            } else {
                counter = 0;
                ArrayList[] myTaskParams = new ArrayList[1];
                myTaskParams[0] = buffer1;
                calcTask = new CalcBackground().execute(myTaskParams);
                buffer1 = buffer2;
                buffer2 = new ArrayList<>();
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }


    private class SaveFile extends AsyncTask<ArrayList, Object, Object> {

        @Override
        protected Object doInBackground(ArrayList... arrayLists) {
            if (!isExternalStorageWritable()) {
                return null;
            }
            ArrayList<String> toSave = arrayLists[0];
            Log.d("saving", arrayLists.toString());
            String pathToSave = getExternalFilesDir(null).getPath();
            File file = new File(pathToSave, "values-" + fileIncrementer + ".csv");
            try {
                FileOutputStream stream = new FileOutputStream(file);
                try {
                    for (String a : toSave) {
                            String output = a.toString() + "\n";
                            stream.write(output.getBytes());
                    }
                    SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt("incrementor", fileIncrementer);
                    editor.commit();
                    fileIncrementer++;
                    stream.close();
                } catch (IOException e) {
                    Log.e("External", "External storage write error");
                }
            } catch (FileNotFoundException e) {
                Log.e("File", "File not found");
            }
            return null;
        }
    }

    public void startRecording(View view) {
        start = true;
    }

    public void stopRecording(View view) {
        start = false;
        ArrayList[] myTaskParams = new ArrayList[1];
        myTaskParams[0] = data;
        myAsyncTask = new SaveFile().execute(myTaskParams);
    }

    private double maxMagnitude(ArrayList<Position> arrayList) {
        double max = 0;
        boolean b = true;
        for (Position p : arrayList) {
            if (b) {
                max = magnitude(p);
                b = false;
            } else {
                double tmp = magnitude(p);
                if (tmp > max) {
                    max = tmp;
                }
            }
        }
        return max;
    }

    private double minMagnitude(ArrayList<Position> arrayList) {
        double min = 0;
        boolean b = true;
        for (Position p : arrayList) {
            if (b) {
                min = magnitude(p);
                b = false;
            } else {
                double tmp = magnitude(p);
                if (tmp < min) {
                    min = tmp;
                }
            }
        }
        return min;
    }

    private double stdDiviation(ArrayList<Position> arrayList) {
        double diviation = 0;
        double n = arrayList.size();
        double sum = 0;
        for (Position p : arrayList) {
            sum += magnitude(p);
        }
        double avg = sum / n;
        double sum2 = 0;
        for (Position p : arrayList) {
            sum2 = Math.pow(magnitude(p) - avg, 2);
        }
        diviation = Math.sqrt((1 / (n - 1)) * sum2);
        Log.d("diviation", "n: " + n + " sum: " + sum + " avg: " + avg + " sum2: " + sum2 + " diviation: " + diviation);
        return diviation;
    }

    private double magnitude(Position p) {
        return Math.abs(Math.sqrt(Math.pow(p.getX(), 2) + (Math.pow(p.getY(), 2) + (Math.pow(p.getZ(), 2)))));
    }

    private class CalcBackground extends AsyncTask<ArrayList, ArrayList, String> {

        @Override
        protected String doInBackground(ArrayList... arrayLists) {
            ArrayList<Position> toCalc = arrayLists[0];
            String toReturn = toCalc.get(0).getTimestamp() +","+ maxMagnitude(toCalc) +","+ minMagnitude(toCalc) +","+ stdDiviation(toCalc) +","+ lable;
            return toReturn;
        }

        @Override
        protected void onPostExecute(String arrayList) {
            calculateBack(arrayList);
        }
    }

    public void calculateBack(String s) {
        data.add(s);
        if(data.size() > 500) {
            ArrayList[] myTaskParams = new ArrayList[1];
            myTaskParams[0] = data;
            myAsyncTask = new SaveFile().execute(myTaskParams);
            data = new ArrayList<>();
        }
        String[] array = s.split(",");
        textView.setText("");
        textView.append("max: " + array[1] + "\n");
        textView.append("min: " + array[2] + "\n");
        textView.append("std. d: " + array[3] + "\n");
        textView.append("lable: " + lable);



    }

    public void toggle(View view){
        textView.setText("");
        if(lable == Lable.HAND){
            lable = Lable.BIKE;
        } else if (lable == Lable.BIKE){
            lable = Lable.CAR;
        } else if (lable == Lable.CAR){
            lable = Lable.TRAIN;
        } else if (lable == Lable.TRAIN){
            lable = Lable.POCKET;
        } else if (lable == Lable.POCKET){
            lable = Lable.HAND;
        }
        textView.append("lable: " + lable);
    }

    private enum Lable{POCKET,HAND,BIKE,CAR,TRAIN}

}
