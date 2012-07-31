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

class Create(args: Array[String]) {
  if (!credentialPath.exists) {
    println("%s not found\nUse 'auth add' subcommand to create".format(credentialPath.path))
    System.exit(-1)
  }

  args.length match {
    case 1 => // create
      // Create bucket specified in .s3 file in this directory or parent; error if no .s3 file
      // Error if bucket exists
      findS3File() match {
        case None =>
          println("No .s3 file found; run this command and specify the awsAccountName and bucketName")
          System.exit(-2)

        case Some(file) =>
          val s3File: S3File = parseS3File(file)
          s3fileTos3Option(s3File) match {
            case None =>
              println("No authentication credentials stored in .aws")
              System.exit(-4)

            case Some(s3) =>
              if (bucketExists(s3File.bucketName)(s3)) {
                println("The bucket '%s' exists for AWS account %s".format(s3File.bucketName, s3File.accountName))
                System.exit(-3)
              }
              s3.createBucket(s3File.bucketName)
              println("Created bucket %s for AWS account %s".format(s3File.bucketName, s3File.accountName))
          }
      }

    case 3 => // create accountName bucketName
      // Error if bucket exists else create bucket
      // if .s3 file exists in current directory or parent, modify it by setting bucketName
      // else tell user that they could run link command
      findS3File() match {
        case None =>
          val accountName = args(1)
          val bucketName = args(2)
          getAuthentication(accountName) match {
            case None =>
              println("Error: Authentication credentials not found in %s; try the link subcommand".format(credentialPath))
              System.exit(-4)

            case Some(credentials) =>
              val s3 = new S3(credentials.accessKey, credentials.secretKey)
              try {
                s3.createBucket(bucketName)
                writeS3(S3File(accountName, bucketName, None))
              } catch {
                case ex =>
                  println(ex.getMessage)
              }
          }

        case Some(file) =>
          println("%s file found; run this command but do not specify the awsAccountName and bucketName, or move to another directory".format(file.getCanonicalPath))
          System.exit(-2)
      }

    case _ =>
      println("Error: Too many arguments provided for link")
      help
  }
}
