package com.server.real;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by brorbw on 15/12/16.
 */

@RestController
public class NotificationController {

    private ArrayList<Notification> notifications = new ArrayList<>();


    private final AtomicLong counter = new AtomicLong();

    @RequestMapping("/notification")
    public Notification notification(){
        //return new Notification(counter.incrementAndGet(), "This is a title", "This is a message");
        if(!notifications.isEmpty()) {
            Notification notification = notifications.get(notifications.size() - 1);
            notifications.remove(notifications.size() - 1);
            return notification;
        } else {
            return new Notification(0, "","");
        }
    }

    @RequestMapping("/add-notification")
    public void setNotifications(@RequestParam(value = "title", defaultValue = "no title") String name,
                                 @RequestParam(value = "message", defaultValue = "no message") String messagge){
        System.out.println("adding notification: " + name + " : " + messagge);
        notifications.add(new Notification(counter.incrementAndGet(),
                name,messagge));
    }
}
