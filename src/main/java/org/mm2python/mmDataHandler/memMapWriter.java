/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mm2python.mmDataHandler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;

import mmcorej.TaggedImage;
import org.mm2python.UI.reporter;
import org.mm2python.mmDataHandler.Exceptions.NoImageException;
import org.micromanager.data.Image;

/**
 *
 * @author bryant.chhun
 */
public final class memMapWriter {

    private memMapWriter() {}

    /**
     * Class with methods to write data to existing memory mapped files.
     * Several methods exist for each of the different data types
     *
     * @param tempImg_: micrmanager Image object
     * @param buffer_: existing MappedByteBuffer
     * @param position_: buffer position, for large buffers
     * @throws NoImageException
     */

    public static void writeToMemMap(Object tempImg_, MappedByteBuffer buffer_, int position_) throws NoImageException {
        byte[] byteImg;
        byteImg = convertToByte(tempImg_);
        if (byteImg == null) {
            throw new NoImageException("image not converted to byte[]");
        }
        writeToMemMapAt(byteImg, buffer_, position_);
    }


    private static byte[] convertToByte(Object tempImg_) throws UnsupportedOperationException {
        try
        {
            byte[] bytes;
            Object pixels; // = tempImg_.getRawPixels();

            if (tempImg_ instanceof Image) {
                Image im = (Image)tempImg_;
                pixels = im.getRawPixels();
            } else if (tempImg_ instanceof TaggedImage) {
                TaggedImage tim = (TaggedImage)tempImg_;
                pixels = tim.pix;
            } else {
                pixels = tempImg_;
            }

            if (pixels instanceof byte[]) {
                bytes = (byte[]) pixels;
            } else if (pixels instanceof short[]) {
                ShortBuffer shortPixels = ShortBuffer.wrap((short[]) pixels);
                ByteBuffer dest = ByteBuffer.allocate(2 * ((short[]) pixels).length).order(ByteOrder.nativeOrder());
                ShortBuffer shortDest = dest.asShortBuffer();
                shortDest.put(shortPixels);
                bytes = dest.array();
            }
            else {
                throw new UnsupportedOperationException("Unsupported pixel type");
            }
            return bytes;
            
        } catch (Exception ex) {
            reporter.set_report_area(ex.toString());
            return null;
        }
    }

    private static void writeToMemMapAt(byte[] byteImg_, MappedByteBuffer buffer, int position) {
        try
        {
            buffer.position(position);
            buffer.put(byteImg_, 0, byteImg_.length);
            buffer.force();
        } catch (Exception ex) {
            reporter.set_report_area("!! Exception !! during write to memmap = "+ex);
            throw ex;
        }
    }

}


    