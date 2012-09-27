package com.micronautics.aws;

import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Mike Slinn
 */
public class Model extends S3Model {
    public static String bucketName;
    public static LinkedList<S3ObjectSummary> allNodes = new LinkedList<>();
    public static List<Pattern> ignoredPatterns = new LinkedList<>();
    public static boolean s3ObjectDataFetched = false;
    public static boolean multithreadingEnabled = false;
    public static ArrayList<Long> modificationTimes = new ArrayList<>();
    public static ArrayList<Long> deletionTimes = new ArrayList<>();
}
