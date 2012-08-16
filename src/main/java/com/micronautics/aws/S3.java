package com.micronautics.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.StringInputStream;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;

/**
 * When uploading, a key does not start with a slash, one is added for consistency with how web clients fetch assets
 * <pre>GET /robots.txt
 * GET /flex/portfolio/healthCare.jsp
 * GET /StyleSheet.css
 * GET /</pre>
 * Similarly, if a non-empty prefix is specified, a slash is also added. This means that all assets uploaded with this
 * program have leading slashes.
 *
 * When downloading, keys are returned with leading dots to be compatible with file systems.
 * For example:
 * <tt></tt>/</tt> translates to <tt>./</tt>
 * <tt></tt>/healthCare.jsp</tt> translates to <tt>./healthCare.jsp</tt>
 * <tt></tt>/flex/portfolio/healthCare.jsp</tt> translates to <tt>./flex/portfolio/healthCare.jsp</tt>
 *
 * Keys of assets that were uploaded by other clients might not start with leading slashes; those assets can
 * not be fetched by web browsers.
 */
public class S3 {
    private AmazonS3Client s3;
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

    // todo create policy for intranets
    protected String bucketPolicy(String bucketName) {
        return "{\n" +
                "\t\"Version\": \"2008-10-17\",\n" +
                "\t\"Statement\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"Sid\": \"AddPerm\",\n" +
                "\t\t\t\"Effect\": \"Allow\",\n" +
                "\t\t\t\"Principal\": {\n" +
                "\t\t\t\t\"AWS\": \"*\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"Action\": \"s3:GetObject\",\n" +
                "\t\t\t\"Resource\": \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                "\t\t}\n" +
                "\t]\n" +
                "}";
    }

    /** Create a new S3 bucket, make it publicly viewable and enable it as a web site.
     * Amazon S3 bucket names are globally unique, so once a bucket repoName has been
     * taken by any user, you can't create another bucket with that same repoName.
     *
     * You can optionally specify a location for your bucket if you want to keep your data closer to your applications or users. */
    public Bucket createBucket(String bucketName) {
        Bucket bucket = s3.createBucket(bucketName);
        s3.setBucketPolicy(bucketName, bucketPolicy(bucketName));
        enableWebsite(bucketName);
        return bucket;
    }

    public void enableWebsite(String bucketName) {
        BucketWebsiteConfiguration configuration = new BucketWebsiteConfiguration("index.html");
        s3.setBucketWebsiteConfiguration(bucketName, configuration);
    }

    public void enableWebsite(String bucketName, String errorPage) {
        BucketWebsiteConfiguration configuration = new BucketWebsiteConfiguration("index.html", errorPage);
        s3.setBucketWebsiteConfiguration(bucketName, configuration);
    }

    public String getBucketLocation(String bucketName) {
        return s3.getBucketLocation(bucketName);
    }

    /** List the buckets in the account */
    public String[] listBuckets() {
        LinkedList<String> result = new LinkedList<String>();
        for (Bucket bucket : s3.listBuckets())
            result.add(bucket.getName());
        return result.toArray(new String[result.size()]);
    }

    /** Uploads a file to the specified bucket. The file's last-modified date is applied to the uploaded file.
     * If the key does not start with a slash, one is added for consistency. */
    public PutObjectResult uploadFile(String bucketName, String key, File file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setLastModified(new Date(file.lastModified()));
        metadata.setContentLength(file.length());

        String keyLC = key.toLowerCase().trim();
        if (keyLC.endsWith(".css"))
            metadata.setContentType("text/css");
        else if (keyLC.endsWith(".doc") || keyLC.endsWith(".docx"))
            metadata.setContentType("application/msword");
        else if (keyLC.endsWith(".html") || keyLC.endsWith(".htm")  || keyLC.endsWith(".shtml") || keyLC.endsWith(".jsp") || keyLC.endsWith(".php"))
            metadata.setContentType("text/html");
        else if (keyLC.endsWith(".gif"))
            metadata.setContentType("image/gif");
        else if (keyLC.endsWith(".jpg"))
            metadata.setContentType("image/jpeg");
        else if (keyLC.endsWith(".js"))
            metadata.setContentType("text/javascript");
        else if (keyLC.endsWith(".pdf"))
            metadata.setContentType("application/pdf");
        else if (keyLC.endsWith(".png"))
            metadata.setContentType("image/png");
        else if (keyLC.endsWith(".txt"))
            metadata.setContentType("text/plain");
        else if (keyLC.endsWith(".zip"))
            metadata.setContentType("application/zip");
        else
            metadata.setContentType("text/plain");

        if (key.startsWith("/"))
            key = key.substring(1);
        try {
            InputStream inputStream = new FileInputStream(file);
            return s3.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return new PutObjectResult();
        }
    }

    public PutObjectResult uploadString(String bucketName, String key, String contents) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setLastModified(new Date());
        metadata.setContentLength(contents.length());
        metadata.setContentType("text/html");
        try {
            InputStream inputStream = new StringInputStream(contents);
            return s3.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return new PutObjectResult();
        }
    }

    /** @param key if the key does not start with a slash, one is added
     *  @see <a href="http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/model/ObjectMetadata.html">ObjectMetadata</a> */
    public void uploadStream(String bucketName, String key, InputStream stream, int filesize) {
        while (key.startsWith("/"))
            key = key.substring(1);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(filesize);
        //metadata.setContentType("whatever");
        metadata.setContentEncoding("utf-8");
        //metadata.setCacheControl("cacheControl");
        s3.putObject(new PutObjectRequest(bucketName, key, stream, metadata));
    }

    /** Download an object - if the key does not start with a slash, one is added.
     * When you download an object, you get all of the object's metadata and a
     * stream from which to read the contents. It's important to read the contents of the stream as quickly as
     * possible since the data is streamed directly from Amazon S3 and your network connection will remain open
     * until you read all the data or close the input stream.
     *
     * GetObjectRequest also supports several other options, including conditional downloading of objects
     * based on modification times, ETags, and selectively downloading a range of an object. */
    public InputStream downloadFile(String bucketName, String key) {
        while (key.startsWith("/"))
            key = key.substring(1);
        key = key.replace("//", "/");
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
//        System.out.println("Content-Type: " + object.getObjectMetadata().getContentType());
        return object.getObjectContent();
    }

    /** List objects in given bucketName by prefix.
     * @param prefix A leading slash is enforced if a prefix is specified */
    public String[] listObjectsByPrefix(String bucketName, String prefix) {
        while (null!=prefix && prefix.length()>0 && prefix.startsWith("/"))
            prefix = prefix.substring(1);
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

    /** @param prefix A leading slash is enforced if a prefix is specified
     *  @return collection of S3ObjectSummary; keys are relativized if prefix is adjusted */
    public LinkedList<S3ObjectSummary> getAllObjectData(String bucketName, String prefix) {
        boolean prefixAdjusted = false;
        while (null!=prefix && prefix.length()>0 && prefix.startsWith("/")) {
            prefix = prefix.substring(1);
            prefixAdjusted = true;
        }
        LinkedList<S3ObjectSummary> result = new LinkedList<S3ObjectSummary>();
        boolean more = true;
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix));
        while (more) {
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                if (prefixAdjusted)
                    objectSummary.setKey(relativize(objectSummary.getKey()));
                result.add(objectSummary);
            }
            more = objectListing.isTruncated();
            if (more)
                objectListing = s3.listNextBatchOfObjects(objectListing);
        }
        return result;
    }

    /** @param prefix A leading slash is enforced if a prefix is specified
     * @return ObjectSummary with leading "./", prepended if necessary*/
    public S3ObjectSummary getOneObjectData(String bucketName, String prefix) {
        while (null!=prefix && prefix.length()>0 && prefix.startsWith("/"))
            prefix = prefix.substring(1);
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix));
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            String key = objectSummary.getKey();
            if (key.compareTo(prefix)==0) {
                objectSummary.setKey(relativize(key));
                return objectSummary;
            }
        }
        return null;
    }

    public String getResourceUrl(String bucketName, String key) {
        return s3.getResourceUrl(bucketName, key);
    }

    /** Prepend "." or "./" to key if required so it can be used as a relative file name */
    public static String relativize(String key) {
        String result = key;
        while (result.startsWith("/"))
            result = result.substring(1);
        result = result.replace("//", "/");
        return result;
    }

    /** Delete an object - if they key does not start with a slash, one is added.
     * Unless versioning has been turned on for the bucket, there is no way to undelete an object. */
    public void deleteObject(String bucketName, String key) {
        if (key.startsWith("/"))
            key = key.substring(1);
        s3.deleteObject(bucketName, key);
    }

    /** Delete a bucket - The bucket will automatically be emptied if necessary so it can be deleted. */
    public void deleteBucket(String bucketName) throws AmazonClientException {
        emptyBucket(bucketName);
        s3.deleteBucket(bucketName);
    }

    public void emptyBucket(String bucketName) throws AmazonClientException {
        LinkedList<S3ObjectSummary> items = getAllObjectData(bucketName, null);
        for (S3ObjectSummary item : items)
            s3.deleteObject(bucketName, item.getKey());
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
