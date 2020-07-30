package org.mm2python.DataStructures.Maps;

import org.micromanager.data.DataProvider;
import org.mm2python.mmEventHandler.globalEvents;

import java.util.concurrent.ConcurrentHashMap;

/**
 * to keep track of globalEvents that are registered to EventBus
 * These must be manually unregistered upon disconnection, or they will always receive events.
 */
public class RegisteredGlobalEvents {

    private static ConcurrentHashMap<DataProvider, globalEvents> RegisteredGlobalEvents;

    static {
        RegisteredGlobalEvents = new ConcurrentHashMap<>(100, 0.75f, 30);
    }

    public static void put(DataProvider ds, globalEvents de) {
        RegisteredGlobalEvents.put(ds, de);
    }

    public static ConcurrentHashMap<DataProvider, globalEvents> getMap() {
        return RegisteredGlobalEvents;
    }

    public static globalEvents get(DataProvider ds) {
        return RegisteredGlobalEvents.get(ds);
    }

    public static void remove(DataProvider ds) {
        RegisteredGlobalEvents.remove(ds);
    }

    public static void reset() {
        RegisteredGlobalEvents.clear();
        RegisteredGlobalEvents = null;
        RegisteredGlobalEvents = new ConcurrentHashMap<>(100, 0.75f, 30);
    }

    public static int getSize() {
        return RegisteredGlobalEvents.size();
    }
}
