package com.yathindra.downloadmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadPool {

    private final ObservableList<DownloadThread> downloadThreads = FXCollections.observableArrayList();
    DownloadStorage downloadStorage = new DownloadStorage();
    
    public DownloadPool() {
        downloadStorage.load();
        this.load();
    }

    /**
     * This saves the current download list to the disk.
     */
    public void save(){
        downloadStorage.clear();
        for (DownloadThread downloadThread:downloadThreads){
            DownloadInfo download;
            download=new DownloadInfo(downloadThread.getDownloadMetadata(),downloadThread.download.getValue().getPartMetadatas());
            downloadStorage.addDownload(download);
        }
        downloadStorage.save();
    }

    /**
     * This loads the download list from the disk.
     * Creates a DownloadPool object that contains all downloads
     *  @return Returns the DownloadPool object that contains all downloads from disk
     */
    public DownloadPool load() {
        if(downloadStorage.getDownloads()==null){return this;}
        for (DownloadInfo downloadInfo : downloadStorage.getDownloads()) {
            
                DownloadMetadata downloadMetadata=downloadInfo.downloadMetadata;
                List<DownloadPartMetadata> downloadPartMetadata=downloadInfo.downloadPartMetadata;
                ConcurrentLinkedQueue queueCommand = new ConcurrentLinkedQueue();
                ConcurrentLinkedQueue queueResponse = new ConcurrentLinkedQueue();
                DownloadRunnable downloadRunnable = new DownloadRunnable(downloadMetadata, queueCommand, queueResponse, downloadPartMetadata);
//                download.loadDownloadPartMetadatas(downloadPartMetadata);
                Thread thread = new Thread(downloadRunnable);
                DownloadThread downloadThread = new DownloadThread(downloadMetadata, downloadRunnable, thread, queueCommand, queueResponse);
                downloadThreads.add(downloadThread);
                thread.start();

            
        }
        return this;
    }

    /**
     * Checks if a URL is valid.
     * @param url String representation of the URL
     * @return If a URL is valid
     */
    public boolean isValidUrl(String url) {
        try {
            URL test=new URL(url);
            return true;
        } catch (MalformedURLException ex) {
            return false;
        }
    }
    
    public ObservableList<DownloadThread> getDownloadThreads() {
        return downloadThreads;
    }

    /**
     * Waits until response of a command is recieved.
     * @param downloadThread The thread to which the command is issued
     * @param response The response from the command.
     */
    private void waitUntilCommand(DownloadThread downloadThread,DownloadAction.Response response){
        if (!downloadThread.thread.isAlive()) {
            return;
        }
        while (true) {
            if(!downloadThread.queueResponse.isEmpty()){
                 if(downloadThread.queueResponse.peek().equals(response)){
                     downloadThread.queueResponse.poll();
                     break;
                 }
            }
        }
    }

    /**
     * Issues a command to the thread.
     * @param downloadThread The object to issue a command to
     * @param command The command to be issued.
     */
    private void issueCommand(DownloadThread downloadThread, DownloadAction.Command command){
        if (!downloadThread.thread.isAlive()) {
            return;
        }
        downloadThread.queueCommand.add(command);
    }

    /**
     * Stops the download from a particular DownloadThread
     * @param downloadThread The download thread to be stopped.
     */
    public void stopDownload(DownloadThread downloadThread) {
        issueCommand(downloadThread,DownloadAction.Command.STOP);
        waitUntilCommand(downloadThread,DownloadAction.Response.STOPPED);
        joinThread(downloadThread);

    }

    /**
     * Pauses the download from a particular DownloadThread.
     * @param downloadThread The download thread to be paused.
     */
    public void pauseDownload(DownloadThread downloadThread) {
        issueCommand(downloadThread, DownloadAction.Command.PAUSE);
        waitUntilCommand(downloadThread,DownloadAction.Response.PAUSED);
    }
    /**
     * Resumes the download from a particular DownloadThread.
     * @param downloadThread The download thread to be resumed.
     */

    public void resumeDownload(DownloadThread downloadThread) {
        issueCommand(downloadThread, DownloadAction.Command.RESUME);
        waitUntilCommand(downloadThread,DownloadAction.Response.RESUMED);
    }

    /**
     * Stops and removes the download from pool.
     * @param downloadThread The download thread to be removed.
     */

    public void removeDownload(DownloadThread downloadThread){
        if(downloadThread.thread.isAlive()){
            stopDownload(downloadThread);
        }
        downloadThreads.remove(downloadThread);
    }

    /**
     * Stops all downloads.
     */

    public void stopAll() {
        for (DownloadThread downloadThread : downloadThreads) {
            stopDownload(downloadThread);
        }
    }

    /**
     * Joins a thread of the downloadThread object.
      * @param downloadThread The downloadThread object to be joined
     */
    public void joinThread(DownloadThread downloadThread){
        try {
                downloadThread.thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(DownloadPool.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    /**
     * Joins all downloadThread objects.
     */
    public void joinThreads() {
        for (DownloadThread downloadThread : downloadThreads) {
            joinThread(downloadThread);
        }
    }

    /**
     * Starts a new download.
     * @param url The download URL
     */
    public void newDownload(String url) {
        DownloadMetadata downloadMetadata;
        try {
            downloadMetadata = new DownloadMetadata(url, downloadThreads.size());
        } catch (MalformedURLException ex) {
            Logger.getLogger(DownloadManager.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        ConcurrentLinkedQueue queueCommand = new ConcurrentLinkedQueue();
        ConcurrentLinkedQueue queueResponse = new ConcurrentLinkedQueue();
        DownloadRunnable downloadRunnable = new DownloadRunnable(downloadMetadata, queueCommand, queueResponse);
        Thread thread = new Thread(downloadRunnable);
        DownloadThread downloadThread = new DownloadThread(downloadMetadata, downloadRunnable, thread, queueCommand, queueResponse);
        downloadThreads.add(downloadThread);
        thread.start();
    }

}

