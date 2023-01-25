package com.yathindra.downloadmanager;

import javafx.beans.property.SimpleObjectProperty;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

public class DownloadMetadata{
    private final SimpleObjectProperty<URL> url;
    private final SimpleObjectProperty<Integer> downloadID;
    private final SimpleObjectProperty<String> filename;
    private static final int parts=8;
    private final SimpleObjectProperty<Long> size=new SimpleObjectProperty<>();
    private static final int timeout=10000;
    private final SimpleObjectProperty<Boolean> accelerated=new SimpleObjectProperty<>(false);
    private final SimpleObjectProperty<DownloadStatus> status=new SimpleObjectProperty<>(DownloadStatus.NEW);

    /**
     * Constructor.
     *
     * @param url The Download URL
     * @param ID The unique integer that represents the download
     * @throws MalformedURLException If URL is improper exception is thrown.
     */
    public DownloadMetadata(String url,int ID) throws MalformedURLException{
        this.url=new SimpleObjectProperty<>(new URL(url));
        this.downloadID=new SimpleObjectProperty(ID);
        String file=String.valueOf(ID)+"_"+Paths.get(this.url.getValue().getPath()).getFileName().toString();
        this.filename=new SimpleObjectProperty<>(file);
    }

    /**
     * Returns the URL object.
     * @return URL object
     */
    public URL getUrl() {
        return url.getValue();
    }

    /**
     * Return the simple object property of URL.
     * @return The simple object of URL
     */
    public SimpleObjectProperty getUrlProperty() {
        return url;
    }

    /**
     * Returns the download ID of the download.
     * @return Download ID
     **/
    public Integer getDownloadID() {
        return downloadID.getValue();
    }

    /**
     * Returns the observable object associated with the downloadID.
     * @return Observable object of the downloadID
     */
    public SimpleObjectProperty<Integer> getDownloadIDProperty() {
        return downloadID;
    }
    
    public String getFilename() {
        return filename.getValue();
    }
    public SimpleObjectProperty getFilenameProperty() {
        return filename;
    }

    public long getSize() {
        return size.getValue();
    }
    
    public SimpleObjectProperty getSizeProperty() {
        return size;
    }
    
    public void setSize(long s){
        size.setValue(s);
    }
    
    public DownloadStatus getStatus() {
        return status.getValue();
    }

    public SimpleObjectProperty getStatusProperty() {
        return status;
    }

    public void setStatus(DownloadStatus status) {
       this.status.setValue(status);
    }
    
    public boolean getAccelerated(){
        return accelerated.getValue();
    }
    
    public SimpleObjectProperty getAcceleratedProperty(){
        return accelerated;
    }
    
    public void setAccelerated(boolean a){
        accelerated.setValue(a);
    }
    public int getTimeout(){
        return timeout;
    }
    public int getParts(){
        return parts;
    }
}
