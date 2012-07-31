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
