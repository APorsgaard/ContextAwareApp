package com.larusaarhus.videonote;

/**
 * Created by brorbw on 22/11/16.
 */
public class Position {
    private double x,y,z;
    private double timestamp;

    public Position(double timestamp, double x, double y, double z){
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public String toString(){
        return timestamp + "," + x + "," + y + "," + z;
    }

}
