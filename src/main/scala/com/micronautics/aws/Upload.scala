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

 package com.micronautics.aws

import com.micronautics.aws.Main._
import scala.Some
import java.nio.file.Paths
import java.io.File
import Upload._
import java.util.regex.Pattern
import collection.JavaConversions._

object Upload {
  /** Upload entire tree to bucket specified in .s3 file in this directory or parent;
    * Error exit if no .aws or .s3 file or if bucket does not exist
    * @return tuple of AWS Credentials, S3File object and File pointing to .s3 */
  def retrieveParams: (Credentials, S3File, File) = {
    findS3File() match {
      case None =>
        println("Error: This directory is not linked with an AWS S3 bucket.\nUse the link subcommand to establish the link.")
        System.exit(0)
        null

      case Some(s3File) =>
        val s3FileObject = parseS3File(s3File) //parse[S3File](Path(file).slurpString(Codec.UTF8))
        getAuthentication(s3FileObject.accountName) match {
          case None =>
            println("Error: %s was not found".format(s3File.getCanonicalPath))
            System.exit(-1)
            null

          case Some(credentials) =>
            val s3 = new S3(credentials.accessKey, credentials.secretKey)
            if (!s3.listBuckets().contains(s3FileObject.bucketName)) {
              println("Error: AWS account %s does not define bucket %s".format(s3FileObject.accountName, s3FileObject.bucketName))
              System.exit(-1)
            }
            (credentials, s3FileObject, s3File)
        }
    }
  }

  /** Continue uploading until Control-C */
  def uploadContinuously(root: File, ignoredPatterns: Seq[Pattern]): Unit = {
    println("Monitoring %s for changes to upload; Control-C to stop".format(root.getCanonicalPath))
    val watchPath = Paths.get(root.getParent)
    new DirectoryWatcher(watchPath, ignoredPatterns).watch()
  }

  def upload(credentials: Credentials, bucketName: String, s3File: File, ignoredPatterns: Seq[Pattern], overwrite: Boolean = true): Unit = {
    new Uploader(credentials, bucketName, ignoredPatterns, overwrite).upload(s3File.getParentFile)
    uploadContinuously(s3File, ignoredPatterns)
  }
}

class Upload(args: Array[String]) {
  if (!credentialPath.exists) {
    println(".aws file not found in %s\nUse 'auth add' subcommand to create".format(credentialPath.path))
    System.exit(-1)
  }

  args.length match {
    case 1 =>
      val (credentials, s3fileObject, s3File) = retrieveParams
      upload(credentials, s3fileObject.bucketName, s3File, s3fileObject.ignoredRegexes)

    case _ =>
      println("Error: Too many arguments provided for upload")
      help
  }
}
