package com.stevenfrew.beatprompter.cloud;

public interface CloudRootPathListener extends CloudListener {
    void onRootPathFound(CloudFolderInfo rootPath);
    void onRootPathError(Throwable t);
}
