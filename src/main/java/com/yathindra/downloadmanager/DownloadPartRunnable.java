package com.yathindra.downloadmanager;

import javafx.beans.property.SimpleObjectProperty;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadPartRunnable implements Runnable {

    private final SimpleObjectProperty<DownloadPartMetadata> metadata;
    private final ConcurrentLinkedQueue queueCommand;
    private final ConcurrentLinkedQueue queueResponse;

    /**
     * Constructor for the DownloadPart object
     * @param metadata DownloadPartMetadata object that contains the metadata of the download part.
     * @param queueCommand A queue object used to communicate with threads. This object gathers commands from thread.
     * @param queueResponse A queue object used to communicate with threads. This object gives responses to thread.
     */
    public DownloadPartRunnable(DownloadPartMetadata metadata, ConcurrentLinkedQueue queueCommand, ConcurrentLinkedQueue queueResponse) {
        this.queueCommand = queueCommand;
        this.queueResponse = queueResponse;
        this.metadata = new SimpleObjectProperty<>(metadata);

    }

    public DownloadPartMetadata getMetadata() {
        return metadata.getValue();
    }

    @Override
    public String toString() {
        return "DownloadPartID:" + getMetadata().partID;
    }

    public DownloadStatus getStatus() {
        return getMetadata().getStatus();
    }

    /**
     * Pause the download.
     */
    public void pause() {
        if (getMetadata().getStatus() == DownloadStatus.DOWNLOADING) {
            getMetadata().setStatus(DownloadStatus.PAUSED);
        }
    }
    /**
     * Resume the download.
     */
    public void resume() {
        if (getMetadata().getStatus() == DownloadStatus.PAUSED) {
            getMetadata().setStatus(DownloadStatus.DOWNLOADING);
        }
    }
    /**
     * Stop the download.
     */
    public void stop() {
        if (getMetadata().getStatus() == DownloadStatus.PAUSED || getMetadata().getStatus() == DownloadStatus.PAUSED) {
            getMetadata().setStatus(DownloadStatus.STOPPED);
        }
    }

    /**
     * Returns the filename of the download file.
     * @return Returns the filename of the download file.
     */
    public String getFilename() {
        return getMetadata().getFilename();
    }

    /**
     * Checks if download is complete
     * @return If download has completed
     */
    public boolean isComplete() {
        return ((getMetadata().getCompletedBytes() + getMetadata().getPart().getStartByte()) == getMetadata().getPart().getEndByte());
    }

    /**
     * Sets up the connection to the download file.
     * @return A stream object that represents the connection to the download file.
     * @throws IOException
     */
    private BufferedInputStream getConnectionStream() throws IOException {
        //Setting up the connection.
        URLConnection connection = getMetadata().downloadMetadata.getUrl().openConnection();
        connection.setRequestProperty("Range", "bytes=" + String.valueOf(getMetadata().getPart().getStartByte() + getMetadata().getCompletedBytes()) + "-" + String.valueOf(getMetadata().getPart().getEndByte()));
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(getMetadata().downloadMetadata.getTimeout());
        connection.connect();

        BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
        return inputStream;
    }

    /**
     * Copies content from one stream to the other.
     * @param inputStream The stream from which to copy
     * @param fileStream The stream to which to copy
     * @return If copy was sucessful returns true. If stop or pause command was issued returns false.
     * @throws IOException
     */
    private boolean copyToStream(BufferedInputStream inputStream, BufferedOutputStream fileStream) throws IOException {
        int byt;
        long completedBytes = getMetadata().getCompletedBytes();

        while ((byt = inputStream.read()) != -1) {
            fileStream.write(byt);
            completedBytes++;
            getMetadata().setCompletedBytes(completedBytes);

            if (!queueCommand.isEmpty()) {
                if (queueCommand.peek().equals(DownloadAction.Command.PAUSE)) {
                    pause();
                    queueCommand.poll();
                    queueResponse.add(DownloadAction.Response.PAUSED);
                    return false;
                } else if (queueCommand.peek().equals(DownloadAction.Command.STOP)) {
                    stop();
                    //I am not adding a poll here because it will stop execution in run thread as well.
                    queueResponse.add(DownloadAction.Response.STOPPED);
                    return false;
                }
            }
        }
        return true;

    }

    /**
     * Start the download of the file.
     * @throws IOException
     * @throws SocketTimeoutException
     */
    private void download() throws IOException, SocketTimeoutException {
        getMetadata().setStatus(DownloadStatus.DOWNLOADING);
        boolean append = (getMetadata().getCompletedBytes() != 0);

        BufferedInputStream inputStream = getConnectionStream();
        BufferedOutputStream fileStream = new BufferedOutputStream(new FileOutputStream(getMetadata().filename, append));
        try {
            if (copyToStream(inputStream, fileStream)) {
                getMetadata().setStatus(DownloadStatus.COMPLETED);
            }
        } finally {
            inputStream.close();
            fileStream.close();
        }

    }

    /**
     * Starts the download of a file. Also handles all the exceptions gracefully.
     */
    public void safeDownload() {
        try {
            download();
        } catch (IOException ex) {
            getMetadata().setStatus(DownloadStatus.ERROR);
            getMetadata().incrementRetries();
            Logger.getLogger(DownloadPartRunnable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Since the class implements runnable, this method is run when run as a thread.
     * When a thread is started it starts the download and waits for commands from the thread.
     * If download is complete or gets completed. it stops and waits for thread to join.
     */
    @Override
    public void run() {
        if (DownloadStatus.COMPLETED == getMetadata().getStatus()) {
            return;
        }
        safeDownload();
        //Infinite loop until the downloadstatus is completed 
        while (getMetadata().getStatus() != DownloadStatus.COMPLETED) {
            //Retry if there is any errors.
            if (getMetadata().getStatus() == DownloadStatus.ERROR) {
                safeDownload();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(DownloadPartRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (!queueCommand.isEmpty()) {
                DownloadAction.Command command = (DownloadAction.Command) queueCommand.poll();
                switch (command) {
                    case STOP:
                        stop();
                        queueResponse.add(DownloadAction.Response.STOPPED);
                        return;
                    case RESUME:
                        resume();
                        queueResponse.add(DownloadAction.Response.RESUMED);
                        safeDownload();
                        break;
                    default:
                        break;
                }

            }
        }
    }
}
