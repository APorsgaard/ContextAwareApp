package com.server.real;

/**
 * Created by brorbw on 15/12/16.
 */
public class Notification {
    private long id;
    private String title;
    private String message;

    public Notification(long id, String title, String message){
        this.id = id;
        this.title=title;
        this.message=message;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}
