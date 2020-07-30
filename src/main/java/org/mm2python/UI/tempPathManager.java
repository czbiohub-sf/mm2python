/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mm2python.UI;

import org.mm2python.DataStructures.Constants;
import org.mm2python.DataStructures.Queues.FixedMemMapReferenceQueue;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * clears all images from the RAMdisk
 * @author bryant.chhun
 */
class tempPathManager {
    private static final JPanel panel = new JPanel();

    private tempPathManager() {}

    /**
     * memory mapped files can not be deleted while the micro-manager process is open
     * Nor can memory mapped files be unmapped until after the process closes
     *
     * This method should be called to resetQueues old memmap file names from earlier runs
     */
    static void clearTempPathContents() {
        File index = new File(Constants.tempFilePath);
        String[] entries = index.list();

        int count = 0;
        int skipped = 0;
        for(String s: entries) {
            try {
                File currentFile = new File(index.getPath(),s);
                currentFile.delete();
                count += 1;
            } catch (Exception ex){
                skipped += 1;
            }
        }

        JOptionPane.showMessageDialog(panel,
                String.format("Removing files from %s\n" +
                "%s files removed\n"+
                "%s files skipped", Constants.tempFilePath, count, skipped));

        FixedMemMapReferenceQueue.resetQueues();
    }

    static void createTempPathFiles(int num_) {
        try {
            FixedMemMapReferenceQueue.createFileNames(num_);
        } catch (FileNotFoundException fex) {
            reporter.set_report_area("exception creating circular memmaps: " + fex.toString());
        }
    }

    static void promptTempPathCreation() {
        int response = JOptionPane.showConfirmDialog(panel,
                String.format("CREATE memory mapped temp files at %s ?", Constants.tempFilePath),
                "CREATE memory mapped temp files?",
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (response == 0) {
            createTempPathFiles(Constants.getNumTempFiles());
        }
    }

    static void promptTempPathDeletion() {
        int response = JOptionPane.showConfirmDialog(panel,
                String.format("DELETE memory mapped temp files at %s ?", Constants.tempFilePath),
                "DELETE memory mapped temp files?",
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (response == 0) {
            clearTempPathContents();
        }
    }
    
}
