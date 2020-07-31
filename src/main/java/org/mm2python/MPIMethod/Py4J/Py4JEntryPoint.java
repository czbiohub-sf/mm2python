/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mm2python.MPIMethod.Py4J;

import mmcorej.TaggedImage;
import org.micromanager.data.Image;
import org.mm2python.DataStructures.Builders.MDSBuilder;
import org.mm2python.DataStructures.Builders.MDSParamBuilder;
import org.mm2python.DataStructures.Builders.MDSParameters;
import org.mm2python.DataStructures.Constants;
import org.mm2python.DataStructures.Maps.MDSMap;
import org.mm2python.DataStructures.MetaDataStore;
//import org.mm2python.DataStructures.Queues.DynamicMemMapReferenceQueue;
import org.mm2python.DataStructures.Queues.FixedMemMapReferenceQueue;
import org.mm2python.DataStructures.Queues.MDSQueue;
import org.mm2python.MPIMethod.zeroMQ.zeroMQ;
import org.mm2python.UI.reporter;
import org.mm2python.mmDataHandler.DataPathInterface;
import org.mm2python.mmDataHandler.DataMapInterface;
import mmcorej.CMMCore;
import org.micromanager.Studio;
import org.mm2python.mmDataHandler.Exceptions.NoImageException;
import org.mm2python.mmDataHandler.memMapWriter;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.security.InvalidParameterException;
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
        return MDSQueue.isQueueEmpty();
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
    public boolean sendFirstImage() {
        try {
            MetaDataStore mds = this.getFirstMeta();
            Object rawpixels = mds.getImage();
            zeroMQ.send(rawpixels);
            return true;
        } catch (Exception ex) {
            return false;
        }
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
                reporter.set_report_area("object representing internal image Buffer is not an instance of Short[] or byte[]");
                return false;
            }
        } catch (Exception ex) {
            reporter.set_report_area(ex.toString());
            return false;
        }
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
        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getLastMDS());
    }

    public MetaDataStore getFirstMeta() {
        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getFirstMDS());
    }

    private MetaDataStore checkMDS(MetaDataStore mds_) {

        if(mds_.getFilepath() == null && mds_.getDataProvider() != null) {
            // mds was acquired by "on demand" method
            // we must write it to the next mem map file and modify the MDS
            try{
                if(FixedMemMapReferenceQueue.isFileQueueEmpty()){
                    FixedMemMapReferenceQueue.createFileNames(Constants.getNumTempFiles());
                }
            } catch(FileNotFoundException FNF) {
                reporter.set_report_area("FileNotFound while creating memory mapped files");
            }
            MappedByteBuffer buffer = FixedMemMapReferenceQueue.getNextBuffer();
            String filename = FixedMemMapReferenceQueue.getNextFileName();

            try {
                Image im = mds_.getDataProvider().getImage(mds_.getCoord());
                memMapWriter.writeToMemMap(im, buffer, 0);
                return new MDSBuilder()
                        .position(mds_.getPosition())
                        .time(mds_.getTime())
                        .z(mds_.getZ())
                        .channel(mds_.getChannel())
                        .xRange(mds_.getxRange())
                        .yRange(mds_.getyRange())
                        .bitDepth(mds_.getBitDepth())
                        .prefix(mds_.getPrefix())
                        .windowname(mds_.getWindowName())
                        .channel_name(mds_.getChannelName())
                        .filepath(filename)
                        .buffer_position(mds_.getBufferPosition())
                        .dataprovider(mds_.getDataProvider())
                        .coord(mds_.getCoord())
                        .summaryMetadata(mds_.getSummaryMetadata())
                        .buildMDS();

            } catch (IOException ioe) {
                reporter.set_report_area("IOException while getting image from DataProvider: " + ioe.toString());
            } catch (NoImageException nie) {
                reporter.set_report_area("NoImageException while writing image to memory mapped file");
            } catch (IllegalAccessException ilex) {
                reporter.set_report_area(String.format("Fail to build MDS for c%d, z%d, p%d, t%d, filepath=%s",
                        mds_.getChannel(), mds_.getZ(), mds_.getPosition(), mds_.getTime(), filename));
            }
        }
        return mds_;
    }


    // =============================================================================


    /**
     * Core Images do not automatically generate MetaData Files.
     *  - Creates MDS based on next available memory mapped file name
     *  - write this MDS to queues/maps, and return this to users
     * @param objim : image Object of type Image, TaggedImage, or Object
     * @return MetaDataStore
     */
    public MetaDataStore getCoreMeta(Object objim) {

        MetaDataStore mds = createMDS();
        MappedByteBuffer buffer = null;
        if(!Constants.getZMQButton()) {
            buffer = FixedMemMapReferenceQueue.getNextBuffer();
        } else {
            reporter.set_report_area("Data transfer mode is set to ZMQ not memory map");
            return null;
        }

        try {
            if(!Constants.getZMQButton()) {
                memMapWriter.writeToMemMap(objim, buffer, mds.getBufferPosition());
            }
            MDSMap.putMDS(mds);
            MDSQueue.putMDS(mds);
        } catch (NoImageException nie) {
            reporter.set_report_area("Attempted to write image, but no image data exists! "
                    +nie.toString());
        } catch (InvalidParameterException ipe) {
            reporter.set_report_area("InvalidParameterException while writing to MetaDataStore HashMap: "
                    +ipe.toString());
        } catch (NullPointerException npe){
            reporter.set_report_area("NullPointerException while writing to MetaDataStore HashMap: "
                    +npe.toString());
        } catch (Exception ex) {
            reporter.set_report_area("General Exception during getCoreMeta "+ex.toString());
        }
        return mds;
    }

    /**
     * build a MDS using:
     *  - memory mapped filename retrieved from queues
     *  - image width, height, bitdepth retrieved from core
     *  - buffer position based on mem map writing type
     * @return MetaDataStore
     */
    private MetaDataStore createMDS() {
        MetaDataStore mds_;
        String filename = null;

        // evaluate data transfer method
        if(!Constants.getZMQButton()) {
            filename = FixedMemMapReferenceQueue.getNextFileName();
        } else {
            reporter.set_report_area("Data transfer mode is set to ZMQ not memory map");
        }

        // create metadata
        try {
            mds_ = new MDSBuilder()
                    .filepath(filename)
                    .buffer_position(0)
                    .xRange((int) mmc.getImageWidth())
                    .yRange((int) mmc.getImageHeight())
                    .bitDepth((int) mmc.getImageBitDepth())
                    .buildMDS();
        } catch (Exception ex) {
            reporter.set_report_area(ex.toString());
            return null;
        }
        return mds_;
    }

//    /**
//     * Query the Memory Mapped Reference Queues for the next available memmap file
//     *
//     * @return MappedByteBuffer : the next available memory mapped file
//     */
//    private MappedByteBuffer getNextBuffer() {
//        MappedByteBuffer buffer = null;
//        // evaluate data transfer method
//        if(!Constants.getZMQButton()) {
//            buffer = FixedMemMapReferenceQueue.getNextBuffer();
//        } else {
//            reporter.set_report_area("Data transfer mode is set to ZMQ not memory map");
//        }
//        return buffer;
//    }

    // ==========================================================================================

    @Override
    public MetaDataStore getLastMetaByChannelName(String channelName) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getLastMDSByParam(params));
    }

    @Override
    public MetaDataStore getLastMetaByChannelIndex(int channelIndex) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();
        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getLastMDSByParam(params));
    }

    @Override
    public MetaDataStore getLastMetaByZ(int z) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();
        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getLastMDSByParam(params));
    }

    @Override
    public MetaDataStore getLastMetaByPosition(int pos) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();
        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getLastMDSByParam(params));
    }

    @Override
    public MetaDataStore getLastMetaByTime(int time) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();
        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getLastMDSByParam(params));
    }

    @Override
    public MetaDataStore getFirstMetaByChannelName(String channelName) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getFirstMDSByParam(params));
    }

    @Override
    public MetaDataStore getFirstMetaByChannelIndex(int channelIndex) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getFirstMDSByParam(params));
    }

    @Override
    public MetaDataStore getFirstMetaByZ(int z) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getFirstMDSByParam(params));
    }

    @Override
    public MetaDataStore getFirstMetaByPosition(int pos) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getFirstMDSByParam(params));
    }

    @Override
    public MetaDataStore getFirstMetaByTime(int time) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return checkMDS(MDSQueue.getFirstMDSByParam(params));
    }

//    public void removeFirstMetaByChannelName(String channelName) throws IllegalAccessException {
//        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
//
//        MDSQueue.removeFirstMDSByParam(params));
//    }
//
//    public void removeLastMetaByChannelName(String channelName) throws IllegalAccessException {
//        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();
//
//        MDSQueue.removeFirstMDSByParam(params));
//    }

    //============== Data Path interface methods ====================//
    //== For retrieving Filepaths to mmap files =====================//


    // Path retrieval interface methods

    @Override
    public String getLastFileByChannelName(String channelName) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return MDSQueue.getLastFilenameByParam(params);
    }

    // get last file by channel
    @Override
    public String getLastFileByChannelIndex(int channelIndex) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return MDSQueue.getLastFilenameByParam(params);
    }

    // get last file by z
    @Override
    public String getLastFileByZ(int z) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return MDSQueue.getLastFilenameByParam(params);
    }

    // get last file by p
    @Override
    public String getLastFileByPosition(int pos) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return MDSQueue.getLastFilenameByParam(params);
    }

    // get last file by t
    @Override
    public String getLastFileByTime(int time) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return MDSQueue.getLastFilenameByParam(params);
    }

    // get first file by channel_name
    @Override
    public String getFirstFileByChannelName(String channelName) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return MDSQueue.getLastFilenameByParam(params);
    }

    // get first file by channel
    @Override
    public String getFirstFileByChannelIndex(int channelIndex) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return MDSQueue.getLastFilenameByParam(params);
    }

    // get first file by z
    @Override
    public String getFirstFileByZ(int z) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return MDSQueue.getLastFilenameByParam(params);
    }

    // get first file by p
    @Override
    public String getFirstFileByPosition(int pos) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return MDSQueue.getLastFilenameByParam(params);
    }

    // get first file by t
    @Override
    public String getFirstFileByTime(int time) throws IllegalAccessException {
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();

        if(MDSQueue.isQueueEmpty()) {
            return null;
        }
        return MDSQueue.getLastFilenameByParam(params);
    }

    //============== Data Map interface methods ====================//
    //== For retrieving MetaDataStore objects and Filenames ======================//
    //== Useful for retrieving but not removing references to existing data ======//

    // Map retrieval interface methods

    @Override
    public ArrayList<String> getAllFilesByChannelName(String channelName) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();

        return MDSMap.getFilenamesByParams(params);
    }

    @Override
    public ArrayList<String> getAllFilesByChannelIndex(int channelIndex) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();

        return MDSMap.getFilenamesByParams(params);
    }

    @Override
    public ArrayList<String> getAllFilesByZ(int z) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();

        return MDSMap.getFilenamesByParams(params);
    }

    @Override
    public ArrayList<String> getAllFilesByPosition(int pos) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();

        return MDSMap.getFilenamesByParams(params);
    }

    @Override
    public ArrayList<String> getAllFilesByTime(int time) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();

        return MDSMap.getFilenamesByParams(params);
    }

    @Override
    public ArrayList<MetaDataStore> getAllMetaByChannelName(String channelName) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel_name(channelName).buildMDSParams();

        return MDSMap.getMDSByParams(params);
    }

    @Override
    public ArrayList<MetaDataStore> getAllMetaByChannelIndex(int channelIndex) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().channel(channelIndex).buildMDSParams();

        return MDSMap.getMDSByParams(params);
    }

    @Override
    public ArrayList<MetaDataStore> getAllMetaByZ(int z) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().z(z).buildMDSParams();

        return MDSMap.getMDSByParams(params);
    }

    @Override
    public ArrayList<MetaDataStore> getAllMetaByPosition(int pos) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().position(pos).buildMDSParams();

        return MDSMap.getMDSByParams(params);
    }

    @Override
    public ArrayList<MetaDataStore> getAllMetaByTime(int time) throws IllegalAccessException{
        MDSParameters params = new MDSParamBuilder().time(time).buildMDSParams();

        return MDSMap.getMDSByParams(params);
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
        return MDSMap.getMDSByParams(mdsp);
    }


}
