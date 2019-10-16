/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mm2python.mmEventHandler;

import org.mm2python.DataStructures.Constants;
import org.mm2python.UI.reporter;
import org.micromanager.Studio;

import com.google.common.eventbus.Subscribe;
import java.util.concurrent.ExecutorService;
import org.mm2python.mmEventHandler.Executor.MainExecutor;
import org.micromanager.events.DisplayAboutToShowEvent;


/**
 *
 * @author bryant.chhun
 */
public class globalEvents {
    private final Studio mm;

    private static final ExecutorService mmExecutor;

    static {
        mmExecutor = MainExecutor.getExecutor();
    }

    /**
     * For registering micro-manager global events.
     *   See here for more details about mm API events:
     *   https://micro-manager.org/wiki/Version_2.0_API_Events
     *
     * @param mm_: micro-manager Studio object generated by the UI
     */
    public globalEvents(Studio mm_) {
        mm = mm_;

        registerGlobalEvents();
        reporter.set_report_area(true, false, false, "global events filename = "+Constants.tempFilePath);
    }

    /**
     * Register this class for notifications from micro-manager.
     */
    public void registerGlobalEvents() {
        mm.events().registerForEvents(this);
    }

    /**
     * Unregister this class for notificaitons from micro-manager
     */
    public void unRegisterGlobalEvents() {
        reporter.set_report_area(true, false, false,"shutting down event monitoring and clearing dequeue references");
        reporter.set_report_area("shutting down global event monitoring");
        mm.events().unregisterForEvents(this);
    }

    /**
     * When this class is registered for events, 'monitor_aboutToShow' receives the
     *  'DisplayAboutToShowEvent' event.
     * Every instance of the display event kicks off a thread using the Singleton mmExecutor
     *
     * @param event: micro-manager event type.
     */
    @Subscribe
    public void monitor_aboutToShow(DisplayAboutToShowEvent event) {
        try {
            reporter.set_report_area(true, true, true, "\n");
            reporter.set_report_area(true, true, true,"DisplayAboutToShowEvent event detected");

            mmExecutor.execute(new globalEventsThread(mm, event.getDisplay()));
        } catch (Exception ex) {
            reporter.set_report_area("EXCEPTION = "+ex.toString());
        }
    }
    
}