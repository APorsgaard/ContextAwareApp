package com.larusaarhus.collectingofdata;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * Created by brorbw on 22/11/16.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String LOG_TAG = "FILE_WRITER";
    private int counter = 0;
    private boolean first = true;
    private ArrayList<Position> buffer1,buffer2;
    private int fileIncrementer;
    private AsyncTask myAsyncTask;
    private AsyncTask calcTask;
    private AsyncTask translate;
    private boolean start;
    private SensorManager sensorManager;
    private ArrayList<ArrayList> data;
    private ArrayList<Position> csv;
    private static int NUMBER_OFF_FILES = 0; //the number of files go here

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        buffer1 = new ArrayList<>();
        buffer2 = new ArrayList<>();
        data = new ArrayList<>();
        csv = new ArrayList<>();
        start = false;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        fileIncrementer = sharedPref.getInt("incrementor", 0);
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
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
        if(start) {
            float[] values = sensorEvent.values;
            if (first) {
                if (counter < 64) {
                    buffer1.add(new Position(sensorEvent.timestamp, values[0], values[1], values[2]));
                    counter++;
                } else {
                    first = false;
                    counter = 0;
                }
            } else if (counter < 64) {
                buffer1.add(new Position(sensorEvent.timestamp, values[0], values[1], values[2]));
                buffer2.add(new Position(sensorEvent.timestamp, values[0], values[1], values[2]));
                counter++;
            } else {
                counter = 0;
                data.add(buffer1);
                ArrayList[] myTaskParams = new ArrayList[1];
                myTaskParams[0] = buffer1;
                calcTask = new CalcBackground().execute(myTaskParams);
                buffer1 = buffer2;
                buffer2 = new ArrayList<>();
            }
            Log.d("SENSOR!!!", "x: " + values[0] + ", y:" + values[1] + ", z:" + values[2]);
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


    private class SaveFile extends AsyncTask<ArrayList,Object,Object> {

        @Override
        protected Object doInBackground(ArrayList... arrayLists) {
            if(!isExternalStorageWritable()){
                return null;
            }
            ArrayList<ArrayList> toSave = arrayLists[0];
            Log.d("saving", arrayLists.toString());
            String pathToSave = getExternalFilesDir(null).getPath();
            File file = new File(pathToSave,  "csv-" + fileIncrementer + ".csv");
            try {
                FileOutputStream stream = new FileOutputStream(file);
                try {
                    for(ArrayList a : toSave) {
                        for (Object p : a) {
                            String output = p.toString() + "\n";
                            stream.write(output.getBytes());
                        }
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

    public void startRecording(View view){
        start = true;
    }
    public void stopRecording(View view){
        start = false;
        ArrayList[] myTaskParams = new ArrayList[1];
        myTaskParams[0] = data;
        myAsyncTask = new SaveFile().execute(myTaskParams);
    }


    private double maxMagnitude(ArrayList<Position> arrayList){
        double max = 0;
        boolean b = true;
        for(Position p : arrayList){
            if(b){
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
    private double minMagnitude(ArrayList<Position> arrayList){
        double min = 0;
        boolean b = true;
        for(Position p : arrayList){
            if(b){
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

    private double stdDiviation (ArrayList<Position> arrayList){
        double diviation = 0;
        double n = arrayList.size();
        double sum = 0;
        for(Position p : arrayList){
            sum += magnitude(p);
        }
        double avg = sum/n;
        double sum2 = 0;
        for(Position p : arrayList){
            sum2 = Math.pow(magnitude(p)-avg,2);
        }
        diviation = Math.sqrt((1/(n-1))*sum2);
        Log.d("diviation", "n: " + n + " sum: " + sum + " avg: " + avg + " sum2: " + sum2 + " diviation: " + diviation);
        return diviation;
    }

    private double magnitude (Position p){
        return Math.abs(Math.sqrt(Math.pow(p.getX(),2)+(Math.pow(p.getY(),2)+(Math.pow(p.getZ(),2)))));
    }

    private class CalcBackground extends AsyncTask<ArrayList,ArrayList,ArrayList>{

        @Override
        protected ArrayList doInBackground(ArrayList... arrayLists) {
            ArrayList<Position> toCalc = arrayLists[0];
            ArrayList<Double> toReturn = new ArrayList<>();
            toReturn.add(maxMagnitude(toCalc));
            toReturn.add(minMagnitude(toCalc));
            toReturn.add(stdDiviation(toCalc));

            return toReturn;
        }

        @Override
        protected void onPostExecute(ArrayList arrayList) {
            calculateBack(arrayList);
        }
    }

    public void calculateBack(ArrayList<Position> arrayList){
        TextView textView = (TextView)findViewById(R.id.textView);

        textView.setText("");
        textView.append("max: " + arrayList.get(0) + "\n");
        textView.append("min: " + arrayList.get(1) + "\n");
        textView.append("std. d: " + arrayList.get(2));

    }

    public void calculate(View view){
        calcTask = new TranslateData().execute();

    }
    private class TranslateData extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            FileInputStream is;
            BufferedReader reader;
            String pathToSave = getExternalFilesDir(null).getPath();
            int i = 0;
            while (i < fileIncrementer) {
                final File file = new File(pathToSave, "csv-" + i + ".csv");
                try {
                    if (file.exists()) {
                        is = new FileInputStream(file);
                        reader = new BufferedReader(new InputStreamReader(is));
                        String line = reader.readLine();
                        while (line != null) {
                            line = reader.readLine();
                            String[] lines = line.split(",");
                            csv.add(new Position(Long.valueOf(lines[0]), Double.valueOf(lines[1]), Double.valueOf(lines[2]), Double.valueOf(lines[3])));
                        }
                    }
                } catch (IOException e) {
                    Log.d("Translater", "Something went wrong in the file reader");
                    e.printStackTrace();
                }
            }
            int j = 0;
            try {
                File file = new File(pathToSave, "deviation-" + fileIncrementer + ".csv");
                FileOutputStream stream = new FileOutputStream(file);
                while (j < csv.size() / 128) {
                    ArrayList<Position> toCal = (ArrayList<Position>) csv.subList(j, j + 127);
                    String output = minMagnitude(toCal) + "," + maxMagnitude(toCal) + "," + stdDiviation(toCal);
                    stream.write(output.getBytes());
                }
            } catch (FileNotFoundException e) {
                Log.d("Translator", "Something went wrong in the file writer");
                e.printStackTrace();
            } catch (IOException e) {
                Log.d("Translator", "Something went wrong in the file writer");
                e.printStackTrace();
            }
            return null;
        }
    }
}
