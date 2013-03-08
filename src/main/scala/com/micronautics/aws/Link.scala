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
import com.micronautics.aws.Util._
import java.io.File
import scala.Some

class Link(args: Array[String]) {
  if (!credentialPath.toFile.exists) {
    println(".aws file not found in %s\nUse 'auth add' subcommand to create".format(credentialPath.toAbsolutePath))
    sys.exit(-1)
  }

  args.length match {
    case 0 =>
      // Display .s3 file in this directory or parent
      findS3File(".s3", new File(System.getProperty("user.dir"))) match {
        case None =>
          println("This directory is not associated with an S3 bucket. Specify the account and bucket name to create a link")

        case Some(file) =>
          val s3File = parseS3File(file) //parse[S3File](Path(file).slurpString(Codec.UTF8))
          val synched = s3File.lastSyncOption match {
            case None =>
              "never synched"

            case Some(dateTime) =>
              "last synched " + dtFormat.print(dateTime)
          }
          println("%s is linked with account '%s', bucket '%s' (%s)".format(file.getCanonicalPath, s3File.accountName, s3File.bucketName, synched))
      }

    case 1 =>
      println("An account name and a bucket name must both be specified.")
      help

    case 2 =>
      // Modify .s3 file in current directory or parent by setting accountName and bucketName, or create in current directory
      findS3File() match {
        case None => // create .s3 file in current directory
          val newS3File = S3File(args(0), args(1), None) // set to never synced
          val synced = writeS3(newS3File)
          println("%s is now linked with AWS account '%s', bucket '%s' (%s)".
            format(findS3File().get.getCanonicalPath, newS3File.accountName, newS3File.bucketName, synced))

        case Some(file) =>
          val oldS3File: S3File = parseS3File(file) //parse[S3File](Path(file).slurpString(Codec.UTF8))
          val newS3File: S3File = oldS3File.copy(args(0), args(1)) // set to never synced because we cannot know if it ever was synched previously
          val synced = writeS3(newS3File)
          println("%s is now linked with account '%s', bucket '%s' (%s)".
            format(file.getCanonicalPath, newS3File.accountName, newS3File.bucketName, synced))
      }

    case _ =>
      println("Error: Too many arguments provided for link")
      help
  }
}
