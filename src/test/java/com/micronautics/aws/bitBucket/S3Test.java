package com.micronautics.aws.bitBucket;

import com.micronautics.aws.S3;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class S3Test {
    @Test
    public void keyValue() throws IOException {
        S3 s3 = new S3();
        FileUtils.copyInputStreamToFile(s3.downloadFile("ej-www", "./index.html"), new File("index.html"));
        FileUtils.copyInputStreamToFile(s3.downloadFile("ej-www", "index.html"), new File("indexNoSlash.html"));

        String[] objects = s3.listObjectsByPrefix("ej-www", "/index");
        for (Object o : objects)
            System.out.println(o);

        objects = s3.listObjectsByPrefix("ej-www", "./index");
        for (Object o : objects)
            System.out.println(o);

        objects = s3.listObjectsByPrefix("ej-www", "index");
        for (Object o : objects)
            System.out.println(o);
    }
}
