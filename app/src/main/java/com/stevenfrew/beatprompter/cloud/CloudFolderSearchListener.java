package com.stevenfrew.beatprompter.cloud;

public interface CloudFolderSearchListener extends CloudListener {
    void onCloudItemFound(CloudItemInfo cloudItem);
    void onFolderSearchError(Throwable t);
    void onFolderSearchComplete();
    void onProgressMessageReceived(String message);
}
