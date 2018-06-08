package com.stevenfrew.beatprompter.cloud;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;

public interface CloudStorage {
    String[] AUDIO_FILE_EXTENSIONS=new String[]{"mp3","wav","m4a","wma","ogg","aac"};
    String[] IMAGE_FILE_EXTENSIONS=new String[]{"jpg","png","jpeg","bmp","tif","tiff"};

    void downloadFiles(List<CloudFileInfo> filesToRefresh);

    String getCloudStorageName();

    CloudType getCloudStorageType();

    void readFolderContents(String folderID, boolean includeSubfolders);

    void selectFolder();

    Observable<String> getProgressMessageSource();

    Observable<CloudDownloadResult> getDownloadResultSource();

    Observable<CloudFileInfo> getFolderContentsSource();

    Observable<CloudFolderInfo> getFolderSelectionSource();
}