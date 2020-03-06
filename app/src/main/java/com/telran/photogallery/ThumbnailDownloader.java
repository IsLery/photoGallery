package com.telran.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";

    private static final int MESSAGE_DOWNLOAD = 0;
    private Handler reqHandler;
    private Handler resHandler;
    private ConcurrentMap<T,String> rqMap = new ConcurrentHashMap<>();
    private ThumbnailDownloadListener<T> listener;

    private boolean hasQuit = false;

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        resHandler = responseHandler;
    }

    public void setListener(ThumbnailDownloadListener<T> listener) {
        this.listener = listener;
    }

    @Override
    protected void onLooperPrepared() {
        reqHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + rqMap.
                            get(target));
                    handleRequest(target);
                }
            }
        };
    }

    public boolean quit(){
        hasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG,  "Got a URL: " + url);
        if (url == null){
            rqMap.remove(target);
        }else {
            rqMap.put(target, url);
            reqHandler.obtainMessage(MESSAGE_DOWNLOAD,target)
                    .sendToTarget();
        }
    }

    private void handleRequest(final T target){
        try{
            final String url = rqMap.get(target);
            if (url == null){
                return;
            }
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            resHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (rqMap.get(target) != url || hasQuit){
                        return;
                    }
                    rqMap.remove(target);
                    listener.onThumbnailDownloaded(target,bitmap);
                }
            });

        }catch (IOException io){
            io.getMessage();
        }


    }

    public void clearQueue(){
        reqHandler.removeMessages(MESSAGE_DOWNLOAD);
        rqMap.clear();
    }


    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }
}
