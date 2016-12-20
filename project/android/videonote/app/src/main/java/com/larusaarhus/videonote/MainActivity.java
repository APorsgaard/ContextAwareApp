package com.larusaarhus.videonote;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import weka.classifiers.trees.J48;

public class MainActivity extends AppCompatActivity {

    private J48 classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void loadModel(){
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream("/some/where/j48.model"));
            J48 cls = (J48) ois.readObject();
            classifier = cls;
            ois.close();
        } catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }
}
