/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

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
