package com.stevenfrew.beatprompter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;

interface CloudStorage {
    String[] AUDIO_FILE_EXTENSIONS=new String[]{"mp3","wav","m4a","wma","ogg","aac"};
    String[] IMAGE_FILE_EXTENSIONS=new String[]{"jpg","png","jpeg","bmp","tif","tiff"};

    List<CloudDownloadResult> refreshFiles(List<CachedFile> filesToRefresh);

    String getCloudStorageName();

    CloudType getCloudStorageType();

    List<CloudDownloadResult> downloadFolderContents(String folderID, boolean includeSubfolders, Map<String,File> existingCachedFiles) throws IOException;

    Observable<String> getProgressMessageSource();
}