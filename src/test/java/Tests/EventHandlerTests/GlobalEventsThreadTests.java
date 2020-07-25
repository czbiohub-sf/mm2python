package Tests.EventHandlerTests;

import org.junit.jupiter.api.Test;
import org.micromanager.LogManager;
import org.micromanager.Studio;
import org.micromanager.acquisition.AcquisitionManager;
import org.micromanager.acquisition.SequenceSettings;
import org.micromanager.data.DataProvider;
import org.micromanager.data.Datastore;
//import org.micromanager.display.DisplayWindow;
import org.micromanager.display.DataViewer;
import org.mm2python.UI.reporter;
import org.mm2python.mmEventHandler.globalEventsThread;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class GlobalEventsThreadTests {

    private Studio mm;
//    private DisplayWindow dw;
    private DataViewer dv;

    private void setUp() {
        mm = mock(Studio.class);
        dv = mock(DataViewer.class);

        // mocking methods needed by reporter
        JTextArea jta = mock(JTextArea.class);
        LogManager lm = mock(LogManager.class);
        when(mm.logs()).thenReturn(lm);
        doNothing().when(lm).logMessage(anyString());

        when(dv.getName()).thenReturn("TEST: window name");
        new reporter(jta, mm);

        // mocking methods needed by testrunnable
        Datastore ds = mock(Datastore.class);
        DataProvider dp = mock(DataProvider.class);
        when(dv.getDatastore()).thenReturn(ds);
        when(dv.getDataProvider()).thenReturn(dp);

        SequenceSettings ss = mock(SequenceSettings.class);
        AcquisitionManager aq = mock(AcquisitionManager.class);
        when(mm.acquisitions()).thenReturn(aq);
        when(aq.getAcquisitionSettings()).thenReturn(ss);
    }

    @Test
    void testConstructor() {
        setUp();
        globalEventsThread geth = new globalEventsThread(mm, dv);

        assertEquals(globalEventsThread.class, geth.getClass());
    }

    @Test
    void testrunnable() {
        setUp();
        globalEventsThread geth = new globalEventsThread(mm, dv);

        geth.run();

    }


}
