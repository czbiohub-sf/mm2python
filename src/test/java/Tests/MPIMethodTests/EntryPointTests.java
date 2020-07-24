package Tests.MPIMethodTests;

import org.junit.jupiter.api.Test;
import org.micromanager.Studio;
import org.micromanager.acquisition.AcquisitionManager;
import org.micromanager.acquisition.SequenceSettings;
import org.micromanager.data.Datastore;
import org.mm2python.DataStructures.Maps.RegisteredDatastores;
import org.mm2python.mmEventHandler.datastoreEvents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class EntryPointTests {

    private Studio mm;
    private Datastore ds;

    private void setUp() {
        mm = mock(Studio.class);
        ds = mock(Datastore.class);
        SequenceSettings ss = mock(SequenceSettings.class);
        AcquisitionManager aq = mock(AcquisitionManager.class);
        when(mm.acquisitions()).thenReturn(aq);
        when(aq.getAcquisitionSettings()).thenReturn(ss);
    }

    private void breakDown() {
        RegisteredDatastores.reset();
    }

    @Test
    void testConstructor() {
        setUp();

        datastoreEvents de = new datastoreEvents(mm, ds, "TEST: window name");

        assertEquals(datastoreEvents.class, de.getClass());

        breakDown();
    }


}
