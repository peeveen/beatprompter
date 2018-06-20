package com.stevenfrew.beatprompter.cloud;

public interface CloudFolderSelectionListener extends CloudListener {
    void onFolderSelected(CloudFolderInfo folderInfo);
    void onFolderSelectedError(Throwable t);
}
