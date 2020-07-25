/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mm2python.MPIMethod.Py4J;

import mmcorej.TaggedImage;
import org.mm2python.DataStructures.Builders.MDSBuilder;
import org.mm2python.DataStructures.Builders.MDSParamBuilder;
import org.mm2python.DataStructures.Builders.MDSParameters;
import org.mm2python.DataStructures.Constants;
import org.mm2python.DataStructures.Maps.MDSMap;
import org.mm2python.DataStructures.MetaDataStore;
import org.mm2python.DataStructures.Queues.DynamicMemMapReferenceQueue;
import org.mm2python.DataStructures.Queues.FixedMemMapReferenceQueue;
import org.mm2python.DataStructures.Queues.MDSQueue;
import org.mm2python.MPIMethod.zeroMQ.zeroMQ;
import org.mm2python.UI.reporter;
import org.mm2python.mmDataHandler.DataPathInterface;
import org.mm2python.mmDataHandler.DataMapInterface;
import org.mm2python.DataStructures.Constants;
import mmcorej.CMMCore;
import org.micromanager.Studio;
import org.mm2python.mmDataHandler.Exceptions.NoImageException;
import org.mm2python.mmDataHandler.memMapFromBuffer;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;


/**
 * 
 * @author bryant.chhun
 */
public class Py4JEntryPoint implements DataMapInterface, DataPathInterface {
    private static Studio mm;
    private static CMMCore mmc;
    private static Py4JListener listener;

    /**
     * constructor
     * @param mm_: the parent studio object.
     */
    Py4JEntryPoint(Studio mm_){
        mm = mm_;
        mmc = mm_.getCMMCore();
        listener = new Py4JListener();

    }

    /**
     * retrieve the micro-manager GUI object
     * @return : Studio
     */
    public Studio getStudio() {
        return mm;
    }

    /**
     * retrieve the micro-manager CORE object
     * @return : CMMCore
     */
    public CMMCore getCMMCore() {
        return mmc;
    }

    /**
     * retrieve the py4j listener object (for implementing java interfaces from python)
     * @return : Py4JListener
     */
    public Py4JListener getListener() {
        return listener;
    }

    /**
     * current camera's image bitdepth
     * @return : int
     */
    public int getBitDepth() {return (int)Constants.bitDepth;}

    /**
     * current camera's image height
     * @return : int
     */
    public int getHeight() {return (int) Constants.height;}

    /**
     * current camera's image width
     * @return : int
     */
    public int getWidth() {return (int) Constants.width;}

    //============== provide utility functions for system management ====//

    /**
     * utility for resetting the queue of acquired images
     * useful when scripting acquisition
     */
    public void clearQueue(){
        MDSQueue.resetQueue();
    }

    /**
     * query if acquired image queue is empty
     * @return : boolean
     */
    public boolean isQueueEmpty(){
        MDSQueue m = new MDSQueue();
        return m.isQueueEmpty();
    }

    /**
     * clears the MDSMap store
     *  the MDSMap store holds all acquired metadata in no particular order
     */
    public void clearMaps() {
        MDSMap.clearData();
    }

    /**
     * query if MDSMap is empty
     * @return : boolean
     */
    public boolean isMapEmpty() {
        return MDSMap.isEmpty();
    }

    /**
     * clear both MDSQueue and MDSMaps
     */
    public void clearAll() {
        MDSQueue.resetQueue();
        MDSMap.clearData();
    }

    //============== zmq data retrieval methods ==================================//
    // TODO: change these to "sendImage" rather than "getImage", which is used in the mmc API
    /**
     * return either the metadata for the last image or send the raw image to zmq ports
     */
    public boolean sendLastImage() {
        try {
            MetaDataStore mds = this.getLastMeta();
            Object rawpixels = mds.getDataProvider().getImage(mds.getCoord()).getRawPixels();
            zeroMQ.send(rawpixels);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * send the first image (as determined by MDS Queue) out via zeroMQ port
     */
    public void sendFirstImage() {
        MetaDataStore mds = this.getFirstMeta();
        Object rawpixels = mds.getImage();
        zeroMQ.send(rawpixels);
    }

    /**
     * send the image (as determined by supplied MDS) out via zeroMQ port
     * @param mds
     */
    public boolean sendImage(MetaDataStore mds) {
        try {
            Object rawpixels = mds.getDataProvider().getImage(mds.getCoord()).getRawPixels();
            zeroMQ.send(rawpixels);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean sendImage(TaggedImage tm) {
        try {
            Object rawpixels = tm.pix;
            zeroMQ.send(rawpixels);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean sendImage(Object obj) {
        try{
            if (obj instanceof short[] || obj instanceof byte[]){
                zeroMQ.send(obj);
                return true;
            } else {
                reporter.set_report_area("object representing internal image Buffer is not an instance of Short[]");
            }
        } catch (Exception ex) {
            reporter.set_report_area(ex.toString());
            return false;
        }
        return false;
    }

    /**
     * get the zeroMQ port
     * @return : String
     */
    public String getZMQPort() {
        return zeroMQ.getPort();
    }

    public ZMQ.Socket getZMQSocket() {
        return zeroMQ.socket;
    }

    public ZContext getZMQContext() {
        return zeroMQ.getContext();
    }

    // =====================================
    // MDSQueue retrieval interface methods

    public MetaDataStore getLastMeta() {
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
//        reporter.set_report_area(String.format("\nLastMeta called (t, z): (%d, %d)", meta.getTime(), meta.getZ()));
        return m.getLastMDS();
    }

    public MetaDataStore getFirstMeta() {
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getFirstMDS();
    }

    // =============================================================================

    public MetaDataStore getCoreMeta(TaggedImage tim) {
        return writeToMemMap(tim);
    }

    public MetaDataStore getCoreMeta(Object objim) {
        return writeToMemMap(objim);
    }

    private MetaDataStore writeToMemMap(Object data_) {
        try {
            MDSMap map = new MDSMap();
            MDSQueue que = new MDSQueue();
            FixedMemMapReferenceQueue fixed = new FixedMemMapReferenceQueue();
            DynamicMemMapReferenceQueue dynamic = new DynamicMemMapReferenceQueue();

            MetaDataStore mds;
            String filename = null;
            MappedByteBuffer buffer = null;
            int buffer_position = 0;

            // evaluate data transfer method
            if(!Constants.getZMQButton()) {
                // assign filename based on type of queue or data source
                if(Constants.getFixedMemMap()) {
                    filename = fixed.getNextFileName();
                    buffer = fixed.getNextBuffer();
                    buffer_position = 0;
                } else {
                    filename = dynamic.getCurrentFileName();
                    buffer = dynamic.getCurrentBuffer();
                    buffer_position = dynamic.getCurrentPosition();
                }
            } else {
                reporter.set_report_area("Data transfer mode is set to ");
            }

            // create metadata
            mds = new MDSBuilder()
                    .filepath(filename)
                    .buffer_position(buffer_position)
                    .xRange((int)mmc.getImageWidth())
                    .yRange((int)mmc.getImageHeight())
                    .bitDepth((int)mmc.getImageBitDepth())
                    .buildMDS();

            // write to memmap
            if(!Constants.getZMQButton()) {
                memMapFromBuffer out = new memMapFromBuffer(data_, buffer);
                out.writeToMemMapAt(buffer_position);
            }

            // write to hashmap
            writeToHashMap(map, mds);

            // write to queues
            writeToQueues(que, mds);

            return mds;

        } catch (NullPointerException ex) {
            reporter.set_report_area("null ptr exception while writing data to memmap");
        } catch (NoImageException ex) {
            reporter.set_report_area(ex.toString());
        } catch (Exception ex) {
            reporter.set_report_area("EXCEPTION IN WRITE TO MEMMAP: "+ex.toString());
        }
        return null;
    }

    private void writeToHashMap(MDSMap fds_, MetaDataStore mds_) {
        try {
            fds_.putMDS(mds_);
        } catch (Exception ex) {
            reporter.set_report_area(ex.toString());
        }
    }

    private void writeToQueues(MDSQueue mq_, MetaDataStore mds_) {
        try {
            mq_.putMDS(mds_);
        } catch (NullPointerException ex) {
            reporter.set_report_area("null ptr exception writing to LinkedBlockingQueue");
        } catch (Exception ex) {
            reporter.set_report_area(ex.toString());
        }
    }

    // ==========================================================================================

    @Override
    public MetaDataStore getLastMetaByChannelName(String channelName) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastMDSByParam(params);
    }

    @Override
    public MetaDataStore getLastMetaByChannelIndex(int channelIndex) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastMDSByParam(params);
    }

    @Override
    public MetaDataStore getLastMetaByZ(int z) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastMDSByParam(params);
    }

    @Override
    public MetaDataStore getLastMetaByPosition(int pos) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastMDSByParam(params);
    }

    @Override
    public MetaDataStore getLastMetaByTime(int time) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastMDSByParam(params);
    }

    @Override
    public MetaDataStore getFirstMetaByChannelName(String channelName) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getFirstMDSByParam(params);
    }

    @Override
    public MetaDataStore getFirstMetaByChannelIndex(int channelIndex) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getFirstMDSByParam(params);
    }

    @Override
    public MetaDataStore getFirstMetaByZ(int z) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getFirstMDSByParam(params);
    }

    @Override
    public MetaDataStore getFirstMetaByPosition(int pos) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getFirstMDSByParam(params);
    }

    @Override
    public MetaDataStore getFirstMetaByTime(int time) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getFirstMDSByParam(params);
    }

//    public void removeFirstMetaByChannelName(String channelName) throws IllegalAccessException {
//        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
//        MDSQueue m = new MDSQueue();
//        m.removeFirstMDSByParam(params);
//    }
//
//    public void removeLastMetaByChannelName(String channelName) throws IllegalAccessException {
//        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
//        MDSQueue m = new MDSQueue();
//        m.removeFirstMDSByParam(params);
//    }

    //============== Data Path interface methods ====================//
    //== For retrieving Filepaths to mmap files =====================//


    // Path retrieval interface methods

    @Override
    public String getLastFileByChannelName(String channelName) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastFilenameByParam(params);
    }

    // get last file by channel
    @Override
    public String getLastFileByChannelIndex(int channelIndex) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastFilenameByParam(params);
    }

    // get last file by z
    @Override
    public String getLastFileByZ(int z) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastFilenameByParam(params);
    }

    // get last file by p
    @Override
    public String getLastFileByPosition(int pos) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastFilenameByParam(params);
    }

    // get last file by t
    @Override
    public String getLastFileByTime(int time) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastFilenameByParam(params);
    }

    // get first file by channel_name
    @Override
    public String getFirstFileByChannelName(String channelName) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastFilenameByParam(params);
    }

    // get first file by channel
    @Override
    public String getFirstFileByChannelIndex(int channelIndex) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastFilenameByParam(params);
    }

    // get first file by z
    @Override
    public String getFirstFileByZ(int z) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastFilenameByParam(params);
    }

    // get first file by p
    @Override
    public String getFirstFileByPosition(int pos) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastFilenameByParam(params);
    }

    // get first file by t
    @Override
    public String getFirstFileByTime(int time) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();
        MDSQueue m = new MDSQueue();
        if(m.isQueueEmpty()) {
            return null;
        }
        return m.getLastFilenameByParam(params);
    }

    //============== Data Map interface methods ====================//
    //== For retrieving MetaDataStore objects and Filenames ======================//
    //== Useful for retrieving but not removing references to existing data ======//

    // Map retrieval interface methods

    @Override
    public ArrayList<String> getAllFilesByChannelName(String channelName) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
        MDSMap m = new MDSMap();
        return m.getFilenamesByParams(params);
    }

    @Override
    public ArrayList<String> getAllFilesByChannelIndex(int channelIndex) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();
        MDSMap m = new MDSMap();
        return m.getFilenamesByParams(params);
    }

    @Override
    public ArrayList<String> getAllFilesByZ(int z) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();
        MDSMap m = new MDSMap();
        return m.getFilenamesByParams(params);
    }

    @Override
    public ArrayList<String> getAllFilesByPosition(int pos) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();
        MDSMap m = new MDSMap();
        return m.getFilenamesByParams(params);
    }

    @Override
    public ArrayList<String> getAllFilesByTime(int time) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();
        MDSMap m = new MDSMap();
        return m.getFilenamesByParams(params);
    }

    @Override
    public ArrayList<MetaDataStore> getAllMetaByChannelName(String channelName) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
        MDSMap m = new MDSMap();
        return m.getMDSByParams(params);
    }

    @Override
    public ArrayList<MetaDataStore> getAllMetaByChannelIndex(int channelIndex) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();
        MDSMap m = new MDSMap();
        return m.getMDSByParams(params);
    }

    @Override
    public ArrayList<MetaDataStore> getAllMetaByZ(int z) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();
        MDSMap m = new MDSMap();
        return m.getMDSByParams(params);
    }

    @Override
    public ArrayList<MetaDataStore> getAllMetaByPosition(int pos) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();
        MDSMap m = new MDSMap();
        return m.getMDSByParams(params);
    }

    @Override
    public ArrayList<MetaDataStore> getAllMetaByTime(int time) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();
        MDSMap m = new MDSMap();
        return m.getMDSByParams(params);
    }


    // ==============================================================
    // Methods to return metadatastores based on arbitrary parameters //
    /**
     * get the parameter builder to search on custom subset of parameters
     *  useful if you want to search on 2 or more parameters
     * @return : MDSParamBuilder object
     */
    @Override
    public MDSParamBuilder getParameterBuilder() {
        return new MDSParamBuilder();
    }

    /**
     * get a list of MetaDataStore objects that match the supplied parameters
     *  note: to extract on unhashed parameters, you must filter the results externally
     *      i.e.: arraylist.get(0).windowname == "specific window name"
     * @param mdsp : MDSParameters object
     * @return : Arraylist of MDS (or list in python)
     */
    @Override
    public ArrayList<MetaDataStore> getMetaByParameters(MDSParameters mdsp) {
        return new MDSMap().getMDSByParams(mdsp);
    }


}
