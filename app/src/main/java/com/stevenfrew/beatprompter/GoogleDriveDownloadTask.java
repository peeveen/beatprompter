/*package com.stevenfrew.beatprompter;

import android.os.Handler;
import android.util.Log;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.model.FileList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

class GoogleDriveDownloadTask extends CloudDownloadTask {
    private final static String GOOGLE_DRIVE_FOLDER_MIMETYPE = "application/vnd.google-apps.folder";

    GoogleDriveDownloadTask(File targetFolder, Handler handler, String cloudPath, boolean includeSubFolders, CachedCloudFileCollection currentCache, ArrayList<MIDIAlias> defaultMIDIAliases, ArrayList<CachedCloudFile> filesToUpdate) {
        super(targetFolder, handler, cloudPath, includeSubFolders, currentCache, defaultMIDIAliases, filesToUpdate);
    }

    void downloadFiles(String folderID, boolean includeSubfolders, Map<String, File> existingCachedCloudFiles, ArrayList<DownloadedFile> downloadedFiles) throws IOException {
        List<String> foldersToQuery = new ArrayList<>();
        foldersToQuery.add(folderID);
        List<String> folderNames = new ArrayList<>();
        folderNames.add("");

        while (!foldersToQuery.isEmpty()) {
            String currentFolderID = foldersToQuery.remove(0);
            String currentFolderName = folderNames.remove(0);
            String queryString = "trashed=false and '" + currentFolderID + "' in parents";
            if (!includeSubfolders)
                queryString += " and mimeType != '" + GOOGLE_DRIVE_FOLDER_MIMETYPE + "'";
            com.google.api.services.drive.Drive.Files.List request = GoogleDriveWrapper.getGoogleDriveService().files().list().setQ(queryString).setFields("nextPageToken,files(id,name,mimeType,modifiedTime)");
            do {
                try {
                    FileList children = request.execute();

                    Log.d(BeatPrompterApplication.TAG, "Iterating through contents, seeing what needs updated/downloaded/deleted ...");

                    for (com.google.api.services.drive.model.File child : children.getFiles()) {
                        String fileID = child.getId();
                        Log.d(BeatPrompterApplication.TAG, "File ID: " + fileID);
                        if (includeSubfolders) {
                            String mimeType = child.getMimeType();
                            if (GOOGLE_DRIVE_FOLDER_MIMETYPE.equals(mimeType)) {
                                Log.d(BeatPrompterApplication.TAG, "Adding folder to list of folders to query ...");
                                foldersToQuery.add(fileID);
                                folderNames.add(child.getName());
                                continue;
                            }
                        }

                        String title = child.getName();
                        Log.d(BeatPrompterApplication.TAG, "File title: " + title);
                        String lowerCaseTitle = title.toLowerCase();
                        boolean audioFile = false;
                        boolean imageFile = false;
                        for (String ext : CloudStorage.AUDIO_FILE_EXTENSIONS)
                            if (lowerCaseTitle.endsWith(ext))
                                audioFile = true;
                        for (String ext : CloudStorage.IMAGE_FILE_EXTENSIONS)
                            if (lowerCaseTitle.endsWith(ext))
                                imageFile = true;
                        this.publishProgress(String.format(SongList.getContext().getString(R.string.checking), title));
                        String safeFilename = Utils.makeSafeFilename(fileID);
                        Log.d(BeatPrompterApplication.TAG, "Safe filename: " + safeFilename);
                        File existingLocalFile = existingCachedCloudFiles.get(fileID);
                        boolean downloadRequired = true;
                        Date lastModified = new Date(child.getModifiedTime().getValue());
                        if (existingLocalFile != null) {
                            Date localFileModified = new Date(existingLocalFile.lastModified());
                            Log.d(BeatPrompterApplication.TAG, "Drive File was last modified " + lastModified);
                            Log.d(BeatPrompterApplication.TAG, "Local File was last downloaded " + localFileModified);
                            if (localFileModified.after(lastModified)) {
                                Log.d(BeatPrompterApplication.TAG, "It hasn't changed since last download ... ignoring!");
                                downloadRequired = false;
                                existingCachedCloudFiles.remove(fileID);
                            } else
                                Log.d(BeatPrompterApplication.TAG, "Looks like it has changed since last download ... re-downloading!");
                        } else
                            Log.d(BeatPrompterApplication.TAG, "Appears to be a file that I don't have yet... downloading!");

                        if (downloadRequired) {
                            Log.d(BeatPrompterApplication.TAG, "Downloading now ...");
                            updateDownloadProgress(title);
                            existingLocalFile = downloadGoogleDriveFile(child, safeFilename);
                            existingCachedCloudFiles.remove(fileID);
                        }

                        if (audioFile)
                            mDownloadedAudioFiles.add(new AudioFile(title, existingLocalFile, fileID, lastModified));
                        else if (imageFile)
                            mDownloadedImageFiles.add(new ImageFile(title, existingLocalFile, fileID, lastModified));
                        else
                            downloadedFiles.add(new DownloadedFile(existingLocalFile, fileID, lastModified, currentFolderName));
                    }
                    request.setPageToken(children.getNextPageToken());
                } catch (UserRecoverableAuthIOException uraioe) {
                    GoogleDriveWrapper.recoverAuthorization(uraioe);
                }
            } while (request.getPageToken() != null &&
                    request.getPageToken().length() > 0);
        }
    }

    private File downloadGoogleDriveFile(com.google.api.services.drive.model.File file, String filename) throws IOException {
        File localFile = new File(mTargetFolder, filename);
        InputStream inputStream = getDriveFileInputStream(file);
        FileOutputStream fos = null;
        if (inputStream != null) {
            try {
                Log.d(BeatPrompterApplication.TAG, "Creating new local file, " + localFile.getAbsolutePath());
                fos = new FileOutputStream(localFile);
                Utils.streamToStream(inputStream, fos);
            } finally {
                if (fos != null)
                    try {
                        fos.close();
                    } catch (Exception eee) {
                        Log.e(BeatPrompterApplication.TAG, "Failed to close file output stream.", eee);
                    }
                try {
                    inputStream.close();
                } catch (Exception eee) {
                    Log.e(BeatPrompterApplication.TAG, "Failed to close input stream.", eee);
                }
            }
        }
        return localFile;
    }

    private InputStream getDriveFileInputStream(com.google.api.services.drive.model.File file) {
        try {
            boolean isGoogleDoc = file.getMimeType().startsWith("application/vnd.google-apps.");
            if (isGoogleDoc) {
                boolean isGoogleTextDoc = file.getMimeType().equals("application/vnd.google-apps.document");
                if (isGoogleTextDoc)
                    return GoogleDriveWrapper.getGoogleDriveService().files().export(file.getId(), "text/plain").executeMediaAsInputStream();
                // Ignore spreadsheets, drawings, etc.
            } else
                // Binary files.
                return GoogleDriveWrapper.getGoogleDriveService().files().get(file.getId()).executeMediaAsInputStream();
        } catch (IOException ioe) {
            // An error occurred.
            ioe.printStackTrace();
        }
        return null;
    }

    boolean downloadFile(String fileID, int fileIndex) throws IOException {
        boolean noLongerExists = false;
        try {
            com.google.api.services.drive.model.File file = GoogleDriveWrapper.getGoogleDriveService().files().get(fileID).setFields("id,name,mimeType,trashed,modifiedTime").execute();
            if (!file.getTrashed()) {
                String title = file.getName();
                Log.d(BeatPrompterApplication.TAG, "File title: " + title);
                String lowerCaseTitle = title.toLowerCase();
                boolean dependencyFile = false;
                if (mUpdateType == CloudFileType.Song) {
                    for (String ext : CloudStorage.AUDIO_FILE_EXTENSIONS)
                        if (lowerCaseTitle.endsWith(ext))
                            dependencyFile = true;
                    for (String ext : CloudStorage.IMAGE_FILE_EXTENSIONS)
                        if (lowerCaseTitle.endsWith(ext))
                            dependencyFile = true;
                }
                this.publishProgress(String.format(SongList.getContext().getString(R.string.checking), title));
                String safeFilename = Utils.makeSafeFilename(fileID);
                Log.d(BeatPrompterApplication.TAG, "Safe filename: " + safeFilename);

                Log.d(BeatPrompterApplication.TAG, "Downloading now ...");
                this.publishProgress(String.format(SongList.getContext().getString(R.string.downloading), title));
                File existingLocalFile = downloadGoogleDriveFile(file, safeFilename);
                Date lastModified = new Date(file.getModifiedTime().getValue());
                if (!onFileDownloaded(existingLocalFile, fileID, lastModified, dependencyFile))
                    noLongerExists = true;
            } else if (fileIndex == 0)
                noLongerExists = true;

        } catch (UserRecoverableAuthIOException uraioe) {
            GoogleDriveWrapper.recoverAuthorization(uraioe);
        }
        return noLongerExists;
    }

    @Override
    protected void onPostExecute(Boolean b) {
        try {
            super.onPostExecute(b);
        }
        finally {
            GoogleDriveWrapper.disconnectClient();
        }
    }

    String getCloudStorageName() {
        return SongList.getContext().getString(R.string.google_drive_string);
    }
}

*/