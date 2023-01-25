package com.yathindra.downloadmanager;

public class Part{
    long startByte;
    long endByte;
    
    public Part(long startByte,long endByte){
        this.startByte=startByte;
        this.endByte=endByte;
    }

    public long getStartByte() {
        return startByte;
    }

    public long getEndByte() {
        return endByte;
    }

    @Override
    public String toString(){
        return String.valueOf(startByte)+"-"+String.valueOf(endByte);
    }
    
}
