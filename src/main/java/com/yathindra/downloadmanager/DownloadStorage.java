package com.yathindra.downloadmanager;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadStorage {

    private List<DownloadInfo> downloads = new ArrayList<>();
    private final String filename = "history.dat";
    private final static String DEFAULT_PATH = "./";
    private final String serializedFileDir;

    public DownloadStorage() {
        this(DEFAULT_PATH);
    }

    protected DownloadStorage(String serializedFileDir) {
        this.serializedFileDir = serializedFileDir;
    }

    /**
     * Adds a download to the list to save.
     * @param download DownloadState object which represents the state of download.
     */
    public void addDownload(DownloadInfo download) {
        downloads.add(download);
    }

    /**
     * Returns the list of DownloadState objects.
     * @return All the downloads.
     */
    public List<DownloadInfo> getDownloads() {
        return downloads;
    }

    /**
     * Clears the list of downloads.
     */
    public void clear() {
        downloads = new ArrayList<>();

    }

    /**
     * Saves the current list of downloads to the disk.
     */
    public void save() {
        XStream xstream = new XStream(new StaxDriver());
        String xmlContent = xstream.toXML(downloads);
        try (OutputStreamWriter file = new OutputStreamWriter(new FileOutputStream(serializedFileDir + filename), StandardCharsets.UTF_8)) {
            file.write(xmlContent);
        } catch (IOException ex) {
            Logger.getLogger(DownloadStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Creates an empty file to store download information.
     */
    public void createNewFile() {
        String xmlContent="<?xml version=\"1.0\" ?><list></list>";
        try (OutputStreamWriter file = new OutputStreamWriter(new FileOutputStream(serializedFileDir + filename), StandardCharsets.UTF_8)) {
            file.write(xmlContent);
        } catch (IOException ex) {
            Logger.getLogger(DownloadStorage.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Loads the download list from the disk.
     */
    public void load() {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(serializedFileDir + filename), StandardCharsets.UTF_8)) {

            XStream xstream = new XStream(new StaxDriver());
            downloads = (List<DownloadInfo>) xstream.fromXML(reader);
        } catch (FileNotFoundException ex) {
            createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(DownloadStorage.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
