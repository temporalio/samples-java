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

import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.uber.cadence.activity.Activity;
import com.uber.cadence.workflow.Workflow;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Sample activities implementation.
 */
public class StoreActivitiesImpl implements StoreActivities {

    private final String hostSpecificTaskList;

    public StoreActivitiesImpl(String taskList) {
        this.hostSpecificTaskList = taskList;
    }

    @Override
    public TaskListFileNamePair download(URL url) {
        try {
            byte[] binary = Resources.toByteArray(url);
            File destination = new File(Files.createTempDir(), "downloaded");
            Files.write(binary, destination);
            System.out.println("download activity: downloaded from " + url + " to " + destination.getAbsolutePath());
            return new TaskListFileNamePair(hostSpecificTaskList, destination.getAbsolutePath());
        } catch (IOException e) {
            throw Workflow.wrap(e);
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
