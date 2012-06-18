package com.micronautics.aws;

import org.apache.commons.io.DirectoryWalker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Uploader extends DirectoryWalker<File> {
    int treeRootStrLen;
    Credentials credentials;
    S3 s3;
    String bucketName;

    public Uploader(Credentials credentials, String bucketName) {
        super();
        this.credentials = credentials;
        this.bucketName = bucketName;
        s3 = new S3(credentials.accessKey(), credentials.secretKey());
    }

    public List<File> upload(File treeRoot) throws IOException {
        treeRootStrLen = treeRoot.getCanonicalPath().length();
        ArrayList<File> results = new ArrayList<File>();
        walk(treeRoot, results);
        return results;
    }

    protected void handleFile(File file, int depth, Collection results) {
        try {
            String path = canonicalPath(file);
            if (new ArrayList<String>(Arrays.asList(".s3", ".git", ".aws", ".svn")).contains(file.getName())) {
                System.out.println("Skipping  " + path);
                return;
            }
            System.out.println("Uploading " + path + " to " + bucketName);
            s3.uploadFile(bucketName, path, file);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private String canonicalPath(File file) throws IOException {
        return file.getCanonicalPath().substring(treeRootStrLen).replace('\\', '/');
    }
}
