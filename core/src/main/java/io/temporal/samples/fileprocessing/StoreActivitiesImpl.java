package io.temporal.samples.fileprocessing;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import io.temporal.activity.Activity;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/** Store activities implementation. */
public class StoreActivitiesImpl implements StoreActivities {

  private final String hostSpecificTaskQueue;

  public StoreActivitiesImpl(String taskQueue) {
    this.hostSpecificTaskQueue = taskQueue;
  }

  @Override
  @SuppressWarnings("deprecation")
  public TaskQueueFileNamePair download(URL url) {
    try {
      byte[] binary = Resources.toByteArray(url);
      File destination = new File(Files.createTempDir(), "downloaded");
      Files.write(binary, destination);
      System.out.println(
          "download activity: downloaded from " + url + " to " + destination.getAbsolutePath());
      return new TaskQueueFileNamePair(hostSpecificTaskQueue, destination.getAbsolutePath());
    } catch (IOException e) {
      throw Activity.wrap(e);
    }
  }

  @Override
  public String process(String sourceFile) {
    System.out.println("process activity: sourceFile= " + sourceFile);
    try {
      String processedName = processFileImpl(sourceFile);
      System.out.println("process activity: processed file: " + processedName);
      return processedName;
    } catch (IOException e) {
      throw Activity.wrap(e);
    }
  }

  @SuppressWarnings("deprecation")
  private String processFileImpl(String fileName) throws IOException {
    File inputFile = new File(fileName);
    File inputDir = inputFile.getParentFile();
    File outputFile = new File(inputDir, "processed");
    // We don't really process it, just copy to keep the sample simple.
    Files.copy(inputFile, outputFile);
    return outputFile.getAbsolutePath();
  }

  @Override
  public void upload(String localFileName, URL url) {
    File file = new File(localFileName);
    if (!file.isFile()) {
      throw new IllegalArgumentException("Invalid file type: " + file);
    }
    // Faking upload to simplify sample implementation.
    System.out.println("upload activity: uploaded from " + localFileName + " to " + url);
  }
}
