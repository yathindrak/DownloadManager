package com.yathindra.downloadmanager;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadRunnable implements Runnable {

    private final SimpleObjectProperty<DownloadMetadata> metadata;
    private final List<DownloadPartThread> downloadPartThreads = FXCollections.observableArrayList();
    private final ConcurrentLinkedQueue queueCommand;
    private final ConcurrentLinkedQueue queueResponse;

    public DownloadRunnable(DownloadMetadata metadata, ConcurrentLinkedQueue queueCommand, ConcurrentLinkedQueue queueResponse) {
        this.metadata = new SimpleObjectProperty<>(metadata);
        this.queueCommand = queueCommand;
        this.queueResponse = queueResponse;
    }

    public DownloadRunnable(DownloadMetadata metadata, ConcurrentLinkedQueue queueCommand, ConcurrentLinkedQueue queueResponse, List<DownloadPartMetadata> downloadPartMetadatas) {
        this.metadata = new SimpleObjectProperty<>(metadata);
        this.queueCommand = queueCommand;
        this.queueResponse = queueResponse;
        this.loadDownloadPartMetadatas(downloadPartMetadatas);
    }

    @Override
    public String toString() {
        return "DownloadID:" + metadata.getValue().getDownloadID();
    }

    public DownloadMetadata getDownloadMetadata() {
        return metadata.getValue();
    }

    public List<DownloadPartMetadata> getPartMetadatas() {
        List<DownloadPartMetadata> metadatas = new ArrayList<>();
        for (DownloadPartThread dthread : downloadPartThreads) {
            metadatas.add(dthread.getDownloadPartMetadata());
        }
        return metadatas;
    }

    /**
     * This sets the headers from the HTTP response.
     * Headers such as Accept-Ranges is required to be initialized.
     * @throws IOException Exception is thrown if connection to the url cannot be made.
     */
    public void setHeaders() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) getDownloadMetadata().getUrl().openConnection();
//        curl --head <URL>
        connection.setRequestMethod("HEAD");
        getDownloadMetadata().setSize(connection.getContentLengthLong());
//        https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
        String ranges = connection.getHeaderField("Accept-Ranges");
        if (ranges != null && !ranges.equals("none")) {
            getDownloadMetadata().setAccelerated(true);
            setStatus(DownloadStatus.STARTING);
        }

    }

    /**
     * This loads the DownloadPartMetadata from the object.
     * @param downloadPartMetadatas The object that contains the partial download information,
     */
    public void loadDownloadPartMetadatas(List<DownloadPartMetadata> downloadPartMetadatas) {
        for (DownloadPartMetadata downloadPartMetadata : downloadPartMetadatas) {
            ConcurrentLinkedQueue queueCom = new ConcurrentLinkedQueue();
            ConcurrentLinkedQueue queueRes = new ConcurrentLinkedQueue();
            downloadPartMetadata.setDownloadMetadata(getDownloadMetadata());
            DownloadPartRunnable downloadPart = new DownloadPartRunnable(downloadPartMetadata, queueCom, queueRes);
            downloadPartThreads.add(new DownloadPartThread(downloadPart, downloadPartMetadata, queueCom, queueRes));
        }
    }

    /**
     * This creates the threads for the download parts.
     * It is useful if the metadata was loaded from file.
     */
    public void createDownloadPartThreads() {
        int partID = 0;
        for (Part part : divideDownload()) {
            DownloadPartMetadata part_metadata = new DownloadPartMetadata(getDownloadMetadata(), partID, part);
            ConcurrentLinkedQueue queueCom = new ConcurrentLinkedQueue();
            ConcurrentLinkedQueue queueRes = new ConcurrentLinkedQueue();
            DownloadPartRunnable downloadPart = new DownloadPartRunnable(part_metadata, queueCom, queueRes);
            downloadPartThreads.add(new DownloadPartThread(downloadPart, part_metadata, queueCom, queueRes));
            partID++;
        }

    }

    /**
     * This initializes the download.
     */
    public void initialize() {
        //If download Part Threads is not empty and loaded from file then skip.
        if (downloadPartThreads.isEmpty()) {
            try {
                setHeaders();

            } catch (IOException ex) {
                Logger.getLogger(DownloadRunnable.class.getName()).log(Level.SEVERE, null, ex);
                setStatus(DownloadStatus.ERROR);
                return;
            }
            createDownloadPartThreads();

        }
    }

    /**
     * This divides the download into equal parts.
     * @return The list of parts which the download is divided into.
     */
    private List<Part> divideDownload() {
        List<Part> parts = new ArrayList<>();
        long start = 0;
        double size = (double) getDownloadMetadata().getSize() / getDownloadMetadata().getParts();
        for (int cnt = 0; cnt < getDownloadMetadata().getParts(); cnt++) {
            Part part = new Part(start, (int) Math.round(size * (cnt + 1)));
            parts.add(part);
            start = (int) Math.round(size * (cnt + 1)) + 1;

        }
        return parts;
    }

    private void setStatus(DownloadStatus downloadStatus) {
        getDownloadMetadata().setStatus(downloadStatus);
    }

    public DownloadStatus getStatus() {
        return getDownloadMetadata().getStatus();
    }

    public boolean isDownloaded() {
        for (DownloadPartThread downloadThread : downloadPartThreads) {
            if (downloadThread.getDownloadPart().getStatus() != DownloadStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Joins a thread object handling all exceptions.
     * @param thread The thread to be joined.
     */
    public void joinThread(Thread thread) {
        if (thread != null && !thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(DownloadRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * This joins all the threads from the download parts.
     */
    public void joinThreads() {
        for (DownloadPartThread downloadThread : downloadPartThreads) {
            joinThread(downloadThread.thread);
        }
    }

    /**
     * Waits for a response from a thread.
     * @param dthread The download part thread which is giving the response
     * @param response The response type.
     */
    public void waitUntilResponse(DownloadPartThread dthread, DownloadAction.Response response) {
        while (true) {
            if (!dthread.queueResponse.isEmpty() && dthread.queueResponse.peek().equals(response)) {
                dthread.queueResponse.poll();
                break;
            }
        }

    }

    /**
     * Issues a command and waits for the response.
     * The command is issued to all download part threads
     * @param command The command to issue.
     * @param response The response that should be received.
     */
    public void issueCommand(DownloadAction.Command command,DownloadAction.Response response){
        for (DownloadPartThread dthread : downloadPartThreads) {
            if (dthread.thread==null || !dthread.thread.isAlive()) {
                continue;
            }
            dthread.queueCommand.add(command);
            waitUntilResponse(dthread, response);
        }
    }

    /**
     * Pause the download.
     */
    public void pause() {
        if (getStatus() != DownloadStatus.DOWNLOADING) {
            return;
        }
        issueCommand(DownloadAction.Command.PAUSE,DownloadAction.Response.PAUSED);
        setStatus(DownloadStatus.PAUSED);
    }

    /**
     * Resume the download.
     */
    public void resume() {
        if (getStatus() != DownloadStatus.PAUSED) {
            return;
        }
        issueCommand(DownloadAction.Command.RESUME,DownloadAction.Response.RESUMED);
        setStatus(DownloadStatus.DOWNLOADING);
    }

    /**
     * Stop the download.
     */
    public void stop() {
        if (getStatus() == DownloadStatus.STOPPED) {
            return;
        }
        issueCommand(DownloadAction.Command.STOP,DownloadAction.Response.STOPPED);
        setStatus(DownloadStatus.STOPPED);
    }

    /**
     * Start the download part thread objects.
     */
    public void startDownloadPartThreads() {
        if (!getDownloadMetadata().getAccelerated()) {
            setStatus(DownloadStatus.ERROR);
            return;
        }
        setStatus(DownloadStatus.DOWNLOADING);
        for (DownloadPartThread downloadThread : downloadPartThreads) {
            Thread thread = new Thread(downloadThread.getDownloadPart());
            thread.setName(this.toString() + " " + downloadThread.downloadPart.toString());
            downloadThread.thread = thread;
            thread.start();
        }
    }

    /**
     * Deletes the download part files.
     * @throws IOException Exception occurs if the file could not be deleted or found.
     */
    public void deleteDownloadPartFiles() throws IOException {
        for (DownloadPartThread downloadThread : downloadPartThreads) {
            DownloadPartRunnable downloadPart = downloadThread.getDownloadPart();
            Files.deleteIfExists(Paths.get(downloadPart.getFilename()));
        }
    }

    /**
     * Copies data from one stream to the other.
     * @param outFile The stream to write to.
     * @param inFile The stream to read from.
     * @throws IOException Exception is thrown if cannot read from input stream.
     */
    public void copyToStream(BufferedOutputStream outFile, BufferedInputStream inFile) throws IOException {
        int byt;
        while ((byt = inFile.read()) != -1 && outFile != null) {
            outFile.write(byt);
        }
    }

    /**
     * Joins all the download part after the download is completed.
     */
    public void joinDownloadParts() {
        if (!isDownloaded()) {
            return;
        }
        setStatus(DownloadStatus.JOINING);

        try(BufferedOutputStream outFile = new BufferedOutputStream(new FileOutputStream(getDownloadMetadata().getFilename()))) {
            for (DownloadPartThread downloadThread : downloadPartThreads) {
                DownloadPartRunnable downloadPartRunnable = downloadThread.getDownloadPart();
                try(BufferedInputStream inFile = new BufferedInputStream(new FileInputStream(downloadPartRunnable.getFilename()))){
                    copyToStream(outFile, inFile);
                }
            }
            setStatus(DownloadStatus.COMPLETED);
            deleteDownloadPartFiles();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DownloadRunnable.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DownloadRunnable.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * This loops is run until the download is completed.
     */
    public void downloadLoop(){
        while (!isDownloaded()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                setStatus(DownloadStatus.ERROR);
                Logger.getLogger(DownloadRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (!this.queueCommand.isEmpty()) {
                DownloadAction.Command command = (DownloadAction.Command) this.queueCommand.poll();
                switch (command) {
                    case PAUSE:
                        this.pause();
                        this.queueResponse.add(DownloadAction.Response.PAUSED);
                        break;
                    case STOP:
                        this.stop();
                        this.joinThreads();
                        this.queueResponse.add(DownloadAction.Response.STOPPED);
                        return;
                    case RESUME:
                        this.resume();
                        this.queueResponse.add(DownloadAction.Response.RESUMED);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Since the class implements the runnable interface.
     * This method is run when the class is run as a thread.
     */
    @Override
    public void run() {
        if (getDownloadMetadata().getStatus() == DownloadStatus.COMPLETED) {
            return;
        }
        this.initialize();
        this.startDownloadPartThreads();
        this.downloadLoop();
        this.joinThreads();
        this.joinDownloadParts();
    }

}
