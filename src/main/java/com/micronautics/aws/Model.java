package com.micronautics.aws;

import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Mike Slinn
 */
public class Model {
    public static final int s3FileDoesNotExist = -2;
    public static final int s3FileIsOlderThanLocal = -1;
    public static final int s3FileSameAgeAsLocal = 0;
    public static final int s3FileNewerThanLocal = 1;
    public static final int s3FileDoesNotExistLocally = 2;

    public static Credentials credentials;
    public static S3 s3;
    public static String bucketName;
    public static LinkedList<S3ObjectSummary> allNodes = new LinkedList<S3ObjectSummary>();
    public static List<Pattern> ignoredPatterns = new LinkedList<Pattern>();
    public static boolean s3ObjectDataFetched = false;
}
