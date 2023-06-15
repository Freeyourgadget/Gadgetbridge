package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.downloads;

public interface FileDownloadListener {
    void onDirectoryDownloaded(DirectoryData directoryData);
    void onFileDownloadComplete(int fileIndex, byte[] data);
    void onFileDownloadError(int fileIndex);
    void onDownloadProgress(long remainingBytes);
    void onAllDownloadsCompleted();
}
