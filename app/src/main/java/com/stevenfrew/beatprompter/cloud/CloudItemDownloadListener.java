package com.stevenfrew.beatprompter.cloud;

public interface CloudItemDownloadListener extends CloudListener {
    void onItemDownloaded(CloudDownloadResult result);
    void onProgressMessageReceived(String message);
    void onDownloadError(Throwable t);
    void onDownloadComplete();
}
