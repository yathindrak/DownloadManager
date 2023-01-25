package com.yathindra.downloadmanager;

import java.util.List;

public class DownloadInfo {
    public DownloadMetadata downloadMetadata;
    public List<DownloadPartMetadata> downloadPartMetadata;
    public DownloadInfo(){
        
    }
    public DownloadInfo(DownloadMetadata downloadMetadata, List<DownloadPartMetadata> downloadPartMetadata) {
        this.downloadMetadata = downloadMetadata;
        this.downloadPartMetadata = downloadPartMetadata;
    }


}
