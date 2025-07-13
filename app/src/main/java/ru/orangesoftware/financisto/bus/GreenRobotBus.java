package ru.orangesoftware.financisto.bus;

import org.greenrobot.eventbus.EventBus;

public class GreenRobotBus {

    private static GreenRobotBus INSTANCE;

    public final EventBus bus = new EventBus();

    private GreenRobotBus() {
    }

    public static GreenRobotBus getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GreenRobotBus();
        }
        return INSTANCE;
    }

    public void post(Object event) {
        bus.post(event);
    }

    public void postSticky(Object event) {
        bus.postSticky(event);
    }

    public <T> T removeSticky(Class<T> eventClass) {
        return bus.removeStickyEvent(eventClass);
    }

    public void register(Object subscriber) {
        if (!bus.isRegistered(subscriber)) {
            bus.register(subscriber);
        }
    }

    public void unregister(Object subscriber) {
        bus.unregister(subscriber);
    }

}
