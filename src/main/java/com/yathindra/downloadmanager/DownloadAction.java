package com.yathindra.downloadmanager;

public class DownloadAction {
    enum Command {
        STOP,
        RESUME,
        PAUSE
    }
    enum Response{
        STOPPED,
        RESUMED,
        PAUSED
    }
    
}
