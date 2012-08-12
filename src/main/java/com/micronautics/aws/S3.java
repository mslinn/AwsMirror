package com.micronautics.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;

public class S3 {
    private AmazonS3 s3;
    public Exception exception;
    public AWSCredentials awsCredentials;

    public S3() {
        this.awsCredentials = new AWSCredentials() {
            public String getAWSAccessKeyId() {
                return System.getenv("accessKey");
            }

            public String getAWSSecretKey() {
                return System.getenv("secretKey");
            }
        };
        try {
            if (awsCredentials.getAWSAccessKeyId()!=null && awsCredentials.getAWSSecretKey()!=null) {
                s3 = new AmazonS3Client(awsCredentials);
            } else {
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("AwsCredentials.properties");
                awsCredentials = new PropertiesCredentials(inputStream);
                s3 = new AmazonS3Client(awsCredentials);
            }
        } catch (Exception ex) {
            exception = ex;
        }
    }

    public S3(final String key, final String secret) {
        awsCredentials = new BasicAWSCredentials(key, secret);
        s3 = new AmazonS3Client(awsCredentials);
    }

    /** Create a new S3 bucket - Amazon S3 bucket names are globally unique, so once a bucket repoName has been
     * taken by any user, you can't create another bucket with that same repoName.
     *
     * You can optionally specify a location for your bucket if you want to keep your data closer to your applications or users. */
    public void createBucket(String bucketName) {
        s3.createBucket(bucketName);
    }

    /** List the buckets in the account */
    public String[] listBuckets() {
        LinkedList<String> result = new LinkedList<String>();
        for (Bucket bucket : s3.listBuckets())
            result.add(bucket.getName());
        return result.toArray(new String[result.size()]);
    }

    /** Uploads a file to the specified bucket. The file's last-modified date is applied to the uploaded file */
    public PutObjectResult uploadFile(String bucketName, String key, File file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setLastModified(new Date(file.lastModified()));
        metadata.setContentLength(file.length());
        try {
            InputStream inputStream = new FileInputStream(file);
            return s3.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return new PutObjectResult();
        }
    }

    /** @param key not sure what this is for; might it be a directory name?
     *  @see http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/model/ObjectMetadata.html */
    public void uploadStream(String bucketName, String key, InputStream stream, int filesize) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(filesize);
        //metadata.setContentType("whatever");
        //metadata.setContentEncoding("utf-8");
        //metadata.setCacheControl("cacheControl");
        s3.putObject(new PutObjectRequest(bucketName, key, stream, metadata));
    }

    /** Download an object - When you download an object, you get all of the object's metadata and a
     * stream from which to read the contents. It's important to read the contents of the stream as quickly as
     * possible since the data is streamed directly from Amazon S3 and your network connection will remain open
     * until you read all the data or close the input stream.
     *
     * GetObjectRequest also supports several other options, including conditional downloading of objects
     * based on modification times, ETags, and selectively downloading a range of an object. */
    public InputStream downloadFile(String bucketName, String key) {
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
//        System.out.println("Content-Type: " + object.getObjectMetadata().getContentType());
        return object.getObjectContent();
    }

    /** List objects in given bucketName by prefix. */
    public String[] listObjectsByPrefix(String bucketName, String prefix) {
        LinkedList<String> result = new LinkedList<String>();
        boolean more = true;
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                .withBucketName(bucketName)
                .withPrefix(prefix));
        while (more) {
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries())
                result.add(objectSummary.getKey() + " (size = " + objectSummary.getSize() + ")");
            more = objectListing.isTruncated();
            if (more)
                objectListing = s3.listNextBatchOfObjects(objectListing);
        }
        return result.toArray(new String[result.size()]);
    }

    public LinkedList<S3ObjectSummary> getAllObjectData(String bucketName, String prefix) {
        LinkedList<S3ObjectSummary> result = new LinkedList<S3ObjectSummary>();
        boolean more = true;
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix));
        while (more) {
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries())
                result.add(objectSummary);
            more = objectListing.isTruncated();
            if (more)
                objectListing = s3.listNextBatchOfObjects(objectListing);
        }
        return result;
    }

    public S3ObjectSummary getOneObjectData(String bucketName, String prefix) {
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix));
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries())
            if (objectSummary.getKey().compareTo(prefix)==0)
                return objectSummary;
        return null;
    }

    /** Delete an object - Unless versioning has been turned on for your bucket,
    * there is no way to undelete an object, so use caution when deleting objects. */
    public void deleteObject(String bucketName, String key) {
        s3.deleteObject(bucketName, key);
    }

    /** Delete a bucket - A bucket must be completely empty before it can be deleted, so remember to
     * delete any objects from your buckets before you try to delete them. */
    public void deleteBucket(String bucketName) throws AmazonClientException {
        s3.deleteBucket(bucketName);
    }

    /** Displays the contents of the specified input stream as text.
     * @param input The input stream to display as text.
     * @throws IOException  */
    private static void displayTextInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;

            System.out.println("    " + line);
        }
        System.out.println();
    }
}
