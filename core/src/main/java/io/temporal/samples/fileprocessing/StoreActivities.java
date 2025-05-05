

package io.temporal.samples.fileprocessing;

import io.temporal.activity.ActivityInterface;
import java.net.URL;

@ActivityInterface
public interface StoreActivities {

  final class TaskQueueFileNamePair {
    private String hostTaskQueue;
    private String fileName;

    public TaskQueueFileNamePair(String hostTaskQueue, String fileName) {
      this.hostTaskQueue = hostTaskQueue;
      this.fileName = fileName;
    }

    /** Jackson needs it */
    public TaskQueueFileNamePair() {}

    public String getHostTaskQueue() {
      return hostTaskQueue;
    }

    public String getFileName() {
      return fileName;
    }
  }

  /**
   * Upload file to remote location.
   *
   * @param localFileName file to upload
   * @param url remote location
   */
  void upload(String localFileName, URL url);

  /**
   * Process file.
   *
   * @param inputFileName source file name @@return processed file name
   */
  String process(String inputFileName);

  /**
   * Downloads file to local disk.
   *
   * @param url remote file location
   * @return local task queue and downloaded file name
   */
  TaskQueueFileNamePair download(URL url);
}
