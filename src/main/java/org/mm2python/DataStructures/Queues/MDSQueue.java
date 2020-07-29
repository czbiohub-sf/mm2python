package org.mm2python.DataStructures.Queues;

import org.mm2python.DataStructures.Builders.MDSParamObject;
import org.mm2python.DataStructures.Builders.MDSParameters;
import org.mm2python.DataStructures.MetaDataStore;
import org.mm2python.UI.reporter;
import org.mockito.internal.matchers.Null;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Methods to place / retrieve MetaDataStores in a linkedBlockingQueue
 *
 */
public class MDSQueue {

    private static final LinkedBlockingDeque<MetaDataStore> mdsQueue = new LinkedBlockingDeque<>();

    public static void resetQueue() {
        mdsQueue.clear();
    }

    // methods to add/remove MetaDataStores from lbd
    public static void putMDS(MetaDataStore mds_) {
        try {
            boolean result = mdsQueue.offer(mds_, 10, TimeUnit.MILLISECONDS);
            if(!result){
                reporter.set_report_area(true, true, true, "failure to offer MDS into MDSQueue: "+mds_.toString());
            }
        } catch (InterruptedException iex) {
            reporter.set_report_area(true, true, true,
                    "interrupted exception placing MDS in queue : "+ iex.toString());
        }
    }

    public static boolean isQueueEmpty() {
        return mdsQueue.isEmpty();
    }

    public static MetaDataStore getFirstMDS() {
        // should we use takeFirst()?  This will wait till mds available, while poll returns null
        // poll takes and removes
        return mdsQueue.pollFirst();
    }

    public static MetaDataStore getLastMDS() {
        // should we use takeLast()?  This will wait till mds available, while poll returns null
        // poll takes and removes
        return mdsQueue.pollLast();
    }

    public static MetaDataStore getFirstMDSByParam(MDSParameters mdsp) throws InvalidParameterException {
        return traverseQueueAndRemove(mdsp, true);
    }

    public static MetaDataStore getLastMDSByParam(MDSParameters mdsp) throws InvalidParameterException {
        return traverseQueueAndRemove(mdsp, false);
    }

    public static String getFirstFilenameByParam(MDSParameters mdsp) throws NullPointerException {
        MetaDataStore m = traverseQueueAndRemove(mdsp, true);
        try {
            return m.getFilepath();
        } catch(NullPointerException ne) {
            throw new NullPointerException(ne.toString());
        }
    }

    public static String getLastFilenameByParam(MDSParameters mdsp) throws NullPointerException {
        MetaDataStore m = traverseQueueAndRemove(mdsp, false);
        try {
            return m.getFilepath();
        } catch (NullPointerException ne) {
            throw new NullPointerException(ne.toString());
        }
    }

//    public void removeFirstMDSByParam(MDSParameters mdsp) throws InvalidParameterException {
//        traverseQueueAndRemove(mdsp, true);
//    }
//
//    public void removeLastMDSByParam(MDSParameters mdsp) throws InvalidParameterException {
//        traverseQueueAndRemove(mdsp, false);
//    }

    /**
     * iterates forward or backwards through the mdsQueue to find the next MDS containing requested parameters
     * @param mdsp_ : MDSParameters object
     * @param forward : to traverse forward or backwards
     * @return : an MDS object
     * @throws InvalidParameterException : commented out for now
     */
    private static MetaDataStore traverseQueue(MDSParameters mdsp_, boolean forward) throws InvalidParameterException {
        Iterator<MetaDataStore> itr;
        if(forward) {
            itr = mdsQueue.iterator();
        } else {
            itr = mdsQueue.descendingIterator();
        }
        ArrayList<MDSParamObject> params = mdsp_.getParams();
        while(itr.hasNext()) {
            MetaDataStore temp = itr.next();
            for(MDSParamObject s : params) {
                switch(s.getLabel()){
                    case "TIME":
                        if(temp.getTime() == s.getInt()) {return temp;}
                        break;
                    case "POSITION":
                        if(temp.getPosition() == s.getInt()) {return temp;}
                        break;
                    case "Z":
                        if(temp.getZ() == s.getInt()) {return temp;}
                        break;
                    case "CHANNEL":
                        if(temp.getChannel() == s.getInt()) {return temp;}
                        break;
                    case "CHANNELNAME":
                        if(temp.getChannelName().equals(s.getStr())) {return temp;}
                        break;
                }
            }
        }
        return null;
    }

    private static MetaDataStore traverseQueueAndRemove(MDSParameters mdsp_, boolean forward) throws InvalidParameterException {
        Iterator<MetaDataStore> itr;
        if(forward) {
            itr = mdsQueue.iterator();
        } else {
            itr = mdsQueue.descendingIterator();
        }
        ArrayList<MDSParamObject> params = mdsp_.getParams();
        while(itr.hasNext()) {
            MetaDataStore temp = itr.next();
            for(MDSParamObject s : params) {
                switch(s.getLabel()){
                    case "TIME":
                        if(temp.getTime() == s.getInt()) {
                            mdsQueue.remove(temp);
                            return temp;
                        }
                        break;
                    case "POSITION":
                        if(temp.getPosition() == s.getInt()) {
                            mdsQueue.remove(temp);
                            return temp;
                        }
                        break;
                    case "Z":
                        if(temp.getZ() == s.getInt()) {
                            mdsQueue.remove(temp);
                            return temp;
                        }
                        break;
                    case "CHANNEL":
                        if(temp.getChannel() == s.getInt()) {
                            mdsQueue.remove(temp);
                            return temp;
                        }
                        break;
                    case "CHANNELNAME":
                        if(temp.getChannelName().equals(s.getStr())) {
                            mdsQueue.remove(temp);
                            return temp;
                        }
                        break;
                }
            }
        }
        return null;
    }

}
