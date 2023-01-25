package com.yathindra.downloadmanager;

import javafx.beans.property.SimpleObjectProperty;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DownloadPartThread {

    public Thread thread;
    public SimpleObjectProperty<DownloadPartRunnable> downloadPart;
    public ConcurrentLinkedQueue queueCommand;
    public ConcurrentLinkedQueue queueResponse;
    public SimpleObjectProperty<DownloadPartMetadata> downloadPartMetadata;


    public DownloadPartThread(DownloadPartRunnable downloadPart, DownloadPartMetadata downloadPartMetadata, ConcurrentLinkedQueue queueCommand, ConcurrentLinkedQueue queueResponse) {
        this.downloadPart = new SimpleObjectProperty<>(downloadPart);
        this.downloadPartMetadata = new SimpleObjectProperty<>(downloadPartMetadata);
        this.queueCommand = queueCommand;
        this.queueResponse = queueResponse;
        
    }
    
    public DownloadPartRunnable getDownloadPart(){
        return downloadPart.getValue();
    }
    
    public DownloadPartMetadata getDownloadPartMetadata(){
        return downloadPartMetadata.getValue();
    }
}


