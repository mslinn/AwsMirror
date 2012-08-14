/* Copyright 2012 Micronautics Research Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License. */

package com.micronautics.aws.bitBucket;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.micronautics.aws.Main;
import com.micronautics.aws.S3;
import com.micronautics.aws.S3File;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static com.micronautics.aws.S3.relativize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class S3Test {
    final static String bucketName = "test" + new Date().getTime();
    final static String file1Name = "test.html";
    final static String file2Name = "test2.html";
    final static File file1 = new File(file1Name);
    final static File file2 = new File(file2Name);
    final static S3 s3 = new S3();

    @BeforeClass
    public static void runBeforeClass() {
        s3.createBucket(bucketName);
    }

    @AfterClass
    public static void runAfterClass() {
        s3.deleteBucket(bucketName);
        if (file2.exists())
            file2.delete();
    }

    @After
    public void runAfterTest() {
        s3.emptyBucket(bucketName);
    }

    @Test
    public void keyEquivalence() throws IOException {
        assertTrue("Ensure file to upload can be found", file1.exists());

        s3.uploadFile(bucketName, file1Name, file1);
        S3ObjectSummary item = s3.getOneObjectData(bucketName, file1Name);
        assertTrue("Upload succeeded", null!=item);
        assertTrue("Upload key matches filename", item.getKey().compareTo(relativize(file1Name))==0);

        S3File s3File = Main.readS3File();
        assertTrue(".s3 file not found", null!=s3File);

        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(s3File.endpointUrl());
        HttpResponse response = httpclient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        if (entity != null && entity.getContentLength()>0) {
            String contents = EntityUtils.toString(entity);
            assertEquals(FileUtils.readFileToString(file1), contents);
        }

        FileUtils.copyInputStreamToFile(s3.downloadFile(bucketName, file1Name), file2);
        assertTrue("Ensure downloaded file can be found", file2.exists());
        assertTrue("Ensure downloaded file is complete", file2.length()==file1.length());

        FileUtils.copyInputStreamToFile(s3.downloadFile(bucketName, "/" + file1Name), file2);
        assertTrue("Ensure downloaded file can be found", file2.exists());
        assertTrue("Ensure downloaded file is complete", file2.length() == file1.length());
    }

    @Test(expected = AmazonS3Exception.class)
    public void boom() throws IOException {
        System.out.println("Downloading /" + file1Name + " (should throw AmazonS3Exception)");
        FileUtils.copyInputStreamToFile(s3.downloadFile(bucketName, file1Name), file1);
    }
}
