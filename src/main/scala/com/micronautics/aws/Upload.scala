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

import Main._
import Upload._
import Util._
import java.nio.file.Paths
import java.io.File
import scala.collection.JavaConversions._

object Upload {
  /** Upload entire tree to bucket specified in .s3 file in this directory or parent;
    * Error exit if no .aws or .s3 file or if bucket does not exist
    * @return tuple of AWS Credentials, S3File object and File pointing to .s3 */
  def retrieveParams: (Credentials, S3File, File) = {
    findS3File() match {
      case None =>
        println("Error: This directory is not linked with an AWS S3 bucket.\nUse the link subcommand to establish the link.")
        sys.exit(0)

      case Some(s3File) =>
        val s3FileObject = parseS3File(s3File) //parse[S3File](Path(file).slurpString(Codec.UTF8))
        getAuthentication(s3FileObject.accountName) match {
          case None =>
            println("Error: %s was not found".format(s3File.getCanonicalPath))
            sys.exit(-1)

          case Some(credentials) =>
            val s3 = new S3(credentials.accessKey, credentials.secretKey)
            if (!s3.listBuckets().contains(s3FileObject.bucketName)) {
              println("Error: AWS account %s does not define bucket %s".format(s3FileObject.accountName, s3FileObject.bucketName))
              sys.exit(-1)
            }
            (credentials, s3FileObject, s3File)
        }
    }
  }

  /** Continue uploading until Control-C */
  def uploadContinuously(root: File): Unit = {
    println("Monitoring %s for changes to upload; Control-C to stop".format(root.getCanonicalPath))
    val watchPath = Paths.get(root.getAbsolutePath)
    new DirectoryWatcher(watchPath).watch()
  }

  def upload(s3File: File, overwrite: Boolean = false): Unit = {
    new Uploader(overwrite).upload(s3File.getParentFile)
    uploadContinuously(s3File.getParentFile)
  }
}

class Upload(args: Array[String]) {
  if (!credentialPath.toFile.exists) {
    println(".aws file not found in %s\nUse 'auth add' subcommand to create".format(credentialPath.toAbsolutePath))
    sys.exit(-1)
  }

  args.length match {
    case 0 =>
      val (credentials, s3fileObject, s3File) = retrieveParams
      Model.bucketName = s3fileObject.bucketName
      Model.ignoredPatterns = s3fileObject.ignoredPatterns
      S3Model.credentials = credentials
      S3Model.s3 = s3fileObject.get
      upload(s3File)

    case 1 => // does not upload continuously after finishing
      val (credentials, s3fileObject, s3File) = retrieveParams
      Model.bucketName = s3fileObject.bucketName
      Model.ignoredPatterns = s3fileObject.ignoredPatterns
      S3Model.credentials = credentials
      S3Model.s3 = s3fileObject.get
      val s3DirFile = new File(args(0))
      if (s3DirFile.exists()) {
        if (s3DirFile.isDirectory) {
          new Uploader(true).upload(s3DirFile)
        } else {
          val s3DirPath = s3DirFile.toPath
          val key = if (s3DirPath.isAbsolute) s3File.toPath.relativize(s3DirFile.toPath).toString else s3DirPath.toString
          new UploadOne(key, s3DirFile).call()
          println()
        }
      } else {
        println(s3DirFile.getAbsolutePath + " does not exist.")
        sys.exit(-1)
      }

    case _ =>
      println("Error: Too many arguments provided for upload")
      help
  }
}
