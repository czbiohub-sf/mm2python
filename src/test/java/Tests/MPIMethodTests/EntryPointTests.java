package Tests.MPIMethodTests;

import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import org.junit.jupiter.api.Test;
import org.micromanager.Studio;
import org.micromanager.data.DataProvider;
import org.mm2python.DataStructures.Builders.MDSBuilder;
import org.mm2python.DataStructures.MetaDataStore;
import org.mm2python.MPIMethod.Py4J.Py4JEntryPoint;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class EntryPointTests {

    private Py4JEntryPoint ep;
    private Studio mm;
    private CMMCore mmc;
    private MetaDataStore mds;
    private TaggedImage tm;
    private DataProvider provider;
    private short[] im1;
    private byte[] im2;
    private float[] im3;

    private void setUp() {
        mm = mock(Studio.class);
        mmc = mock(CMMCore.class);
        ep = mock(Py4JEntryPoint.class);
        provider = mock(DataProvider.class);
        when(mm.getCMMCore()).thenReturn(mmc);
    }

    private void setupFakeData() {
        try{
            mds = new MDSBuilder().position(0).z(1).time(2).channel(3).
                    xRange(1024).yRange(1024).bitDepth(16).
                    channel_name("DAPI").prefix("prefix_").windowname("window").
                    filepath("/path/to/this/memorymapfile.dat")
                    .dataprovider(provider)
                    .buildMDS();
            tm = mock(TaggedImage.class);
            im1 = new short[] {1,2,3,4};
            im2 = new byte[] {5,6,7,8};
            im3 = new float[] {9,10,11,12};
        } catch (Exception ex) {
            fail(ex);
        }
    }

    private void breakDown() { }

    @Test
    void testConstructor() {
        setUp();
        assertEquals(Py4JEntryPoint.class, ep.getClass());
    }

    @Test
    void testSendImageMDS() {
        setUp();
        setupFakeData();
        assertFalse(ep.sendImage(mds));
    }

    @Test
    void testSendImageTM() {
        setUp();
        setupFakeData();
        assertFalse(ep.sendImage(tm));
    }

    @Test
    void testSendImageObj() {
        setUp();
        setupFakeData();
        assertFalse(ep.sendImage(im1));
        assertFalse(ep.sendImage(im2));
        ep.sendImage(im3);

    }

}
