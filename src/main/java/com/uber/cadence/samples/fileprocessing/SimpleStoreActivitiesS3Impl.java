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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.uber.cadence.activity.Activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This is an S3 Store implementation which provides Activities to
 * download/upload files from S3
 * 
 */
public class SimpleStoreActivitiesS3Impl implements SimpleStoreActivities {

    private final AmazonS3 s3Client;

    private final String localDirectory;

    private final String hostSpecificTaskList;

    public SimpleStoreActivitiesS3Impl(AmazonS3 s3Client, String localDirectory, String taskList) {
        this.s3Client = s3Client;
        this.localDirectory = localDirectory;
        this.hostSpecificTaskList = taskList;
    }

    @Override
    public void upload(String bucketName, String localName, String targetName) {
        uploadFileToS3(bucketName, localDirectory + localName, targetName);
    }

    /**
     *
     * @param bucket
     *            Name of S3 bucket
     * @param localName
     *            Name of the file to upload
     * @param remoteName
     *            Key for the S3 object
     */

    private void uploadFileToS3(String bucket, String localName, String remoteName) {
        System.out.println("uploadToS3 begin remoteName=" + remoteName + ", localName=" + localName);
        File f = new File(localName);
        s3Client.putObject(bucket, remoteName, f);
        System.out.println("uploadToS3 done");
    }

    @Override
    public String download(String bucketName, String remoteName, String localName) throws Exception {
        return downloadFileFromS3(bucketName, remoteName, localDirectory + localName);
    }

    /**
     *
     * @param bucketName
     *            Name of S3 bucket
     * @param remoteName
     *            Key to use for uploaded S3 object
     * @param localName
     *            Name of the file locally
     * @return host specific task list name
     */

    private String downloadFileFromS3(String bucketName, String remoteName, String localName) throws IOException {
        System.out.println("downloadFileFromS3 begin remoteName=" + remoteName + ", localName=" + localName);
        FileOutputStream f = new FileOutputStream(localName);
        try {
            S3Object obj = s3Client.getObject(bucketName, remoteName);
            InputStream inputStream = obj.getObjectContent();
            long totalSize = obj.getObjectMetadata().getContentLength();

            try {
                long totalRead = 0;
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    totalRead += read;
                    f.write(bytes, 0, read);
                    int progress = (int) (totalRead / totalSize * 100);
                    Activity.heartbeat(progress);
                }
            }
            finally {
                inputStream.close();
            }
        }
        finally {
            f.close();
        }
        // Return hostname file was downloaded to
        System.out.println("downloadFileFromS3 done");
        return hostSpecificTaskList;
    }

    @Override
    public void deleteLocalFile(String fileName) {
        deleteLocalFiles(localDirectory + fileName);
    }

    /**
     *
     * @param fileName
     *            Filename to delete locally
     */

    private void deleteLocalFiles(String fileName) {
        System.out.println("deleteLocalActivity begin fileName=" + fileName);
        File f = new File(fileName);
        f.delete();

        System.out.println("deleteLocalActivity done");
    }
}
