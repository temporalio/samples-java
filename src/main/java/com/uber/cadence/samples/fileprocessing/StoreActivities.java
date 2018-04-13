/*
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

package com.uber.cadence.samples.fileprocessing;

import java.net.URL;

public interface StoreActivities {

  final class TaskListFileNamePair {
    private final String hostTaskList;
    private final String fileName;

    public TaskListFileNamePair(String hostTaskList, String fileName) {
      this.hostTaskList = hostTaskList;
      this.fileName = fileName;
    }

    public String getHostTaskList() {
      return hostTaskList;
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
   * @return local task list and downloaded file name
   */
  TaskListFileNamePair download(URL url);
}
