/*
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.services.simpleworkflow.flow.examples.fileprocessing;

public interface SimpleStoreActivities {
 
    /**
     * 
     * @param localName
     *          Name of the file to upload from temporary directory
     * @param targetName
     *          Name of the file to use on S3 bucket after upload
     * @param bucketName
     *          Name of the S3 bucket
     * @return
     */
    void upload(String bucketName, String localName, String targetName);
    /**
     * 
     * @param remoteName 
     *          Name of the file to download from S3 bucket 
     * @param localName
     *          Name of the file used locally after download
     * @param bucketName
     *          Name of the S3 bucket
     */
    String download(String bucketName, String remoteName, String localName) throws Exception;
    /**
     * 
     * @param fileName 
     *          Name of file to delete from temporary folder
     */
    void deleteLocalFile(String fileName);

}
