package org.mm2python.DataStructures.Maps;

import org.micromanager.data.DataProvider;
import org.mm2python.mmEventHandler.globalEvents;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * to keep track of globalEvents that are registered to EventBus
 * These must be manually unregistered upon disconnection, or they will always receive events.
 */
public class RegisteredGlobalEvents {

    private static LinkedBlockingQueue<globalEvents> RegisteredGlobalEvents;

    static {
        RegisteredGlobalEvents = new LinkedBlockingQueue<>(1);
    }

    public static void put(globalEvents ge) throws InterruptedException {
        RegisteredGlobalEvents.offer(ge, 1, TimeUnit.SECONDS);
    }

    public static globalEvents get(globalEvents ge) {
        return RegisteredGlobalEvents.peek();
    }

    public static void remove(globalEvents ge) {
        RegisteredGlobalEvents.remove(ge);
    }

    public static void reset() {
        RegisteredGlobalEvents.clear();
        RegisteredGlobalEvents = null;
        RegisteredGlobalEvents = new LinkedBlockingQueue<>(1);
    }

    public static int getSize() {
        return RegisteredGlobalEvents.size();
    }

    public static LinkedBlockingQueue<globalEvents> getAllRegisteredGlobalEvents() {
        return RegisteredGlobalEvents;
    }
}
