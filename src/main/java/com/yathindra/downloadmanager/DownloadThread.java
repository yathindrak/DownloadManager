package com.yathindra.downloadmanager;

import javafx.beans.property.SimpleObjectProperty;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DownloadThread {

    public SimpleObjectProperty<DownloadMetadata> downloadMetadata;
    public SimpleObjectProperty<DownloadRunnable> download;
    public Thread thread;
    public ConcurrentLinkedQueue queueCommand;
    public ConcurrentLinkedQueue queueResponse;

    public DownloadThread(DownloadMetadata downloadMetadata, DownloadRunnable downloadRunnable, Thread thread, ConcurrentLinkedQueue queueCommand, ConcurrentLinkedQueue queueResponse) {
        this.downloadMetadata = new SimpleObjectProperty<>(downloadMetadata);
        this.download = new SimpleObjectProperty<>(downloadRunnable);
        this.thread = thread;
        this.queueCommand = queueCommand;
        this.queueResponse = queueResponse;
    }
    
    
    public DownloadRunnable getDownload(){
        return download.getValue();
    }
    public DownloadMetadata getDownloadMetadata() {
        return downloadMetadata.getValue();
    }
}
