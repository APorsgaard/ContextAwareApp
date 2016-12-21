package com.larusaarhus.videonote;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import com.mashape.unirest.http.Unirest;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;

import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.pmml.Array;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private J48 classifier;
    private boolean first = true;
    private SensorManager sensorManager;
    private ArrayList<Position> buffer1, buffer2;
    private int counter = 0;
    private AsyncTask backgroundtask;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadModel();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        buffer1 = new ArrayList<>();
        buffer2 = new ArrayList<>();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Unirest.shutdown();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void loadModel(){
        //loading the model
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    getAssets().open("FinalModel.model"));
            J48 cls = (J48) ois.readObject();
            classifier = cls;
            ois.close();
        } catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
        if(classifier != null){
            Log.d("Model", "loadModel: The model is here");
        }
    }

    @Override
    protected void onStart() {
        //things to be done on start
        super.onStart();
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
    }


    /* ---- SensorEvent ---- */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //when there is data from the sensor
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
                backgroundtask = new BackgroundTask().execute(myTaskParams);
                buffer1 = buffer2;
                buffer2 = new ArrayList<>();
            }
        }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private class BackgroundTask extends AsyncTask<ArrayList, Double, Double>{
        //background thread

        @Override
        protected Double doInBackground(ArrayList[] objects) {
            double time = ((Position)objects[0].get(0)).getTimestamp();
            double max = maxMagnitude(objects[0]);
            double min = minMagnitude(objects[0]);
            double std = stdDiviation(objects[0]);
            //Log.d("Classification", "doInBackground" + " :" + max + " : " + min + " : " + std);
            Instances testData = getInstance(time,max,min,std);
            Instances copy = testData;
            double pred = -1;
            try {
                testData.setClassIndex(testData.numAttributes()-1);
                pred = classifier.classifyInstance(testData.firstInstance());
                copy.firstInstance().setClassValue(pred);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d("Classification", "Lable : " + copy.firstInstance().classAttribute());
            return pred;
        }

        @Override
        protected void onPostExecute(Double o) {
            Log.d("Classification", "onPostExecute: " + o);
        }
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
        //Log.d("diviation", "n: " + n + " sum: " + sum + " avg: " + avg + " sum2: " + sum2 + " diviation: " + diviation);
        return diviation;
    }

    private double magnitude(Position p) {
        return Math.abs(Math.sqrt(Math.pow(p.getX(), 2) + (Math.pow(p.getY(), 2) + (Math.pow(p.getZ(), 2)))));
    }

    public Instances getInstance(Double time, Double max, Double min, Double std) {

        //gettng the instances

        ArrayList<Attribute> atts = new ArrayList<Attribute>(4);
        ArrayList<String> classVal = new ArrayList<String>();



        atts.add(new Attribute("time"));
        atts.add(new Attribute("max"));
        atts.add(new Attribute("min"));
        atts.add(new Attribute("std"));
        classVal.add("BIKE");
        classVal.add("POCKET");
        classVal.add("HAND");
        classVal.add("CAR");
        classVal.add("TRAIN");
        atts.add(new Attribute("@@class@@", classVal));

        Instances dataRaw = new Instances("TestInstances", atts, 0);
        double[] instanceValue = new double[dataRaw.numAttributes()];

        //instanceValue[0] = time;
        instanceValue[0] = max;
        instanceValue[1] = min;
        instanceValue[2] = std;
        instanceValue[3] = 0;

        dataRaw.add(new DenseInstance(1.0, instanceValue));

        dataRaw.setClassIndex(0);

        // return tha Instance packed in an instances object
        return dataRaw;

    }

    public void notifitation(View view){
        getNotification();
        Intent resultIntent = this.getIntent();
// no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_video)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");
        mBuilder.setContentIntent(resultPendingIntent);

        int mNotificationId = 001;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

    }
    public void vibrate(View view){
        Vibrator v = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(500);
    }

    public void getNotification(){
        backgroundtask = new BaackgroundTask().execute();
    }


    private class BaackgroundTask extends AsyncTask<Void, String, String>{

        @Override
        protected String doInBackground(Void... voids) {
            // The connection URL
            String url = "http://videoitnow.com:8080/notification";

// Create a new RestTemplate instance
            RestTemplate restTemplate = new RestTemplate();

// Add the String message converter
            restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

// Make the HTTP GET request, marshaling the response to a String
            String result = restTemplate.getForObject(url, String.class, "Android");
            JSONObject re;
            Log.d("HTTP", "getNotification: " + result);
            try {

                re = new JSONObject(result);
                Log.d("HTTP", "As JSON: " + re.getString("title") + " : " + re.getString("message"));
            } catch (JSONException e){
                e.printStackTrace();
            }

            return result;
        }
    }

}

