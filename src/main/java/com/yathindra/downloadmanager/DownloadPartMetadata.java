package com.yathindra.downloadmanager;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import javafx.beans.property.SimpleObjectProperty;

public class DownloadPartMetadata {
    public SimpleObjectProperty<Integer> partID;
    public SimpleObjectProperty<DownloadStatus> status = new SimpleObjectProperty<>(DownloadStatus.STARTING);
    public String filename;

    //This field will be included multiple time if it is included
    //The annotation lets the field to be omitted by XStream
    @XStreamOmitField
    public DownloadMetadata downloadMetadata;

    public SimpleObjectProperty<Part> part;
    public SimpleObjectProperty<Long> completedBytes = new SimpleObjectProperty<>(0L);
    public SimpleObjectProperty<Integer> retries = new SimpleObjectProperty<>(0);


    public DownloadPartMetadata(DownloadMetadata downloadMetadata, int partID, Part part) {
        this.downloadMetadata = downloadMetadata;
        this.partID = new SimpleObjectProperty<>(partID);
        this.part = new SimpleObjectProperty<>(part);
        this.filename = downloadMetadata.getFilename() + ".part" + String.valueOf(partID);
    }

    public Part getPart() {
        return part.getValue();
    }

    public void setDownloadMetadata(DownloadMetadata downloadMetadata) {
        this.downloadMetadata = downloadMetadata;
    }

    public DownloadStatus getStatus() {
        return status.getValue();
    }

    public void setStatus(DownloadStatus s) {
        status.setValue(s);
    }

    public void setCompletedBytes(long b) {
        completedBytes.setValue(b);
    }

    public long getCompletedBytes() {
        return completedBytes.getValue();
    }

    public void incrementRetries() {
        retries.setValue(retries.getValue() + 1);
    }

    public String getFilename() {
        return filename;
    }


}
