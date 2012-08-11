package com.micronautics.aws;

import akka.dispatch.Await;
import akka.dispatch.Future;
import akka.dispatch.Futures;
import akka.dispatch.MessageDispatcher;
import akka.util.Duration;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.io.DirectoryWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public class Uploader extends DirectoryWalker<File> {
    private Logger logger = LoggerFactory.getLogger(getClass());
    boolean overwrite;
    int treeRootStrLen;
    Credentials credentials;
    S3 s3;
    String bucketName;
    MessageDispatcher dispatcher = Main.system().dispatcher();
    private final ArrayList<Future<PutObjectResult>> futures = new ArrayList<Future<PutObjectResult>>();
    LinkedList<S3ObjectSummary> allNodes;
    List<Pattern> ignoredPatterns;

    public Uploader(Credentials credentials, String bucketName, List<Pattern> ignoredPatterns, boolean overwrite) {
        super();
        this.credentials = credentials;
        this.bucketName = bucketName;
        this.ignoredPatterns = ignoredPatterns;
        this.overwrite = overwrite;
        s3 = new S3(credentials.accessKey(), credentials.secretKey());
    }

    public List<File> upload(File treeRoot) throws IOException {
        // todo share allNodes with Downloader so sync does not fetch all objects twice
        allNodes = s3.getAllObjectData(bucketName, ""); // get every object
        treeRootStrLen = treeRoot.getCanonicalPath().length();
        ArrayList<File> results = new ArrayList<File>();
        walk(treeRoot, results);
        final Future<Iterable<PutObjectResult>> future = Futures.sequence(futures, dispatcher);
        try { // block until the Futures all complete
            Await.result(future, Duration.Inf());
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        return results;
    }

    /** @return true if matching s3 file and it is older, or if there is a read error */
    private boolean s3FileIsOlder(File file, String path) {
        for (S3ObjectSummary node : allNodes) {
            String key = node.getKey();
            try {
                if (key.compareTo(path)==0)
                  return s3FileIsOlder(file, node);
            } catch (Exception e) {
                System.out.println(e.getMessage() + ": " + key);
                return true;
            }
        }
        return false;
    }

    public static boolean s3FileIsOlder(File file, S3ObjectSummary node) {
        if (!file.exists())
            return false;

        Date s3NodeLastModified = node.getLastModified();
        boolean isOlder = s3NodeLastModified.getTime()<file.lastModified();
//        logger.debug("s3NodeLastModified.getTime()=" + s3NodeLastModified.getTime() +
//                "; file.lastModified()=" + file.lastModified() +
//                "; older=" + isOlder);
        return isOlder;
    }

    protected boolean ignore(File file) {
        for (Pattern pattern : ignoredPatterns)
            if (pattern.matcher(file.getName()).matches()) {
                logger.debug("Uploader ignoring " + file.getName());
                return true;
            }
        return false;
    }

    @Override protected boolean handleDirectory(File directory, int depth, Collection results) {
        boolean ignore = ignore(directory);
        if (ignore)
            logger.debug("Uploader ignoring " + directory.getName());
        return !ignore;
    }

    @Override protected void handleFile(File file, int depth, Collection results) {
        try {
            String path = canonicalPath(file);
            boolean s3Older = s3FileIsOlder(file, path);
            //System.out.println("overwrite=" + overwrite + "; s3Older=" + s3Older + "; " + file.getAbsolutePath());
            if (ignore(file)) {
                logger.debug("Uploader ignoring " + path);
                return;
            }
            if (!overwrite && !s3Older) {
                if (s3Older)
                  logger.debug("Uploader skipping " + path + " because the local copy is older");
                else
                    logger.debug("Uploader skipping " + path + " because overwrite is disabled");
                return;
            }
            logger.info("Uploading " + path + " to " + bucketName); // todo display absolute upload path
            final Future<PutObjectResult> future = Futures.future(new UploadOne(bucketName, path, file), dispatcher);
            futures.add(future);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private String canonicalPath(File file) throws IOException {
        String path = file.getCanonicalPath().substring(treeRootStrLen).replace('\\', '/');
        if (path.startsWith("/") || path.startsWith("\\"))
            return path.substring(1);
        else
            return path;
    }

    private class UploadOne implements Callable<PutObjectResult> {
        private String bucketName;
        private String path;
        private File file;

        public UploadOne(String bucketName, String path, File file) {
            this.bucketName = bucketName;
            this.path = path;
            this.file = file;
        }

        @Override
        public PutObjectResult call() {
            return s3.uploadFile(bucketName, path, file);
        }
    }
}
