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

class Download(args: Array[String]) {
  if (!credentialPath.exists) {
    println(".aws file not found in %s\nUse 'auth add' subcommand to create".format(credentialPath.path))
    sys.exit(-1)
  }

  args.length match {
    case 0 =>
      // Upload entire tree to bucket specified in .s3 file in this directory or parent;
      // Error if no .aws or .s3 file or if bucket does not exist
      findS3File() match {
        case None =>
          println("Error: This directory is not linked with an AWS S3 bucket.\nUse the link subcommand to establish the link.")

        case Some(file) =>
          val s3File = parseS3File(file)
          getAuthentication(s3File.accountName) match {
            case None =>
              println("Error: %s was not found".format(file.getCanonicalPath))
              sys.exit(-1)

            case Some(credentials) =>
              val s3 = new S3(credentials.accessKey, credentials.secretKey)
              if (!s3.listBuckets().contains(s3File.bucketName)) {
                println("Error: AWS account %s does not define bucket %s".format(s3File.accountName, s3File.bucketName))
                sys.exit(-1)
              }
              Model.credentials = credentials
              Model.bucketName = s3File.bucketName
              Model.s3 = s3
              new Downloader(true).download(file.getParentFile)
          }
      }

    case _ =>
      println("Error: Too many arguments provided for upload")
      help
  }
}
