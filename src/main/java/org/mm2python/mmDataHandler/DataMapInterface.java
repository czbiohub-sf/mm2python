/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mm2python.mmDataHandler;

import org.mm2python.DataStructures.Builders.MDSParamBuilder;
import org.mm2python.DataStructures.Builders.MDSParameters;
import org.mm2python.DataStructures.MetaDataStore;

import java.util.ArrayList;

/**
 *
 * @author bryant.chhun
 */
public interface DataMapInterface {


    // methods to retrieve from map
    //  these call MDSMap
    //  return arraylists of filenames
    //  return arraylists of MDS objects
    // get files by channel_name
    public ArrayList<String> getAllFilesByChannelName(String channelName) throws IllegalAccessException;

    // get files by channel
    public ArrayList<String> getAllFilesByChannelIndex(int channelIndex) throws IllegalAccessException;

    // get files by z
    public ArrayList<String> getAllFilesByZ(int z) throws IllegalAccessException;

    // get files by p
    public ArrayList<String> getAllFilesByPosition(int pos) throws IllegalAccessException;

    // get files by t
    public ArrayList<String> getAllFilesByTime(int time) throws IllegalAccessException;

    // get meta by channel_name
    public ArrayList<MetaDataStore> getAllMetaByChannelName(String channelName) throws IllegalAccessException;

    // get meta by channel
    public ArrayList<MetaDataStore> getAllMetaByChannelIndex(int channelIndex) throws IllegalAccessException;

    // get meta by z
    public ArrayList<MetaDataStore> getAllMetaByZ(int z) throws IllegalAccessException;

    // get meta by p
    public ArrayList<MetaDataStore> getAllMetaByPosition(int pos) throws IllegalAccessException;

    // get meta by t
    public ArrayList<MetaDataStore> getAllMetaByTime(int time) throws IllegalAccessException;


    //  these call MDSQueue
    //  return a single MDS
    // get last meta by channel_name
    public MetaDataStore getLastMetaByChannelName(String channelName) throws IllegalAccessException;

    // get last meta by channel
    public MetaDataStore getLastMetaByChannelIndex(int channelIndex) throws IllegalAccessException;

    // get last meta by z
    public MetaDataStore getLastMetaByZ(int z) throws IllegalAccessException;

    // get last meta by p
    public MetaDataStore getLastMetaByPosition(int pos) throws IllegalAccessException;

    // get last meta by t
    public MetaDataStore getLastMetaByTime(int time) throws IllegalAccessException;

    // get first meta by channel_name
    public MetaDataStore getFirstMetaByChannelName(String channelName) throws IllegalAccessException;

    // get first meta by channel
    public MetaDataStore getFirstMetaByChannelIndex(int channelIndex) throws IllegalAccessException;

    // get first meta by z
    public MetaDataStore getFirstMetaByZ(int z) throws IllegalAccessException;

    // get first meta by p
    public MetaDataStore getFirstMetaByPosition(int pos) throws IllegalAccessException;

    // get first meta by t
    public MetaDataStore getFirstMetaByTime(int time) throws IllegalAccessException;

    // extras
    //  get parameter builder
    public MDSParamBuilder getParameterBuilder();

    //  get by parameters
    public ArrayList<MetaDataStore> getMetaByParameters(MDSParameters mdsp);

}
