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

/** Create a bucket, enabled for a web site with index file index.html */
class Create(args: Array[String]) {
  if (!credentialPath.exists) {
    println("%s not found\nUse 'auth add' subcommand to create".format(credentialPath.path))
    System.exit(-1)
  }

  args.length match {
    case 1 => // create
      createFromS3File()

    case 3 => // create accountName bucketName
      createFromCmdLine()

    case _ =>
      println("Error: Too many arguments provided for link")
      help
  }


  /**
    * Create bucket specified in .s3 file in this directory or parent; error if no .s3 file
    * Error if bucket exists
    */
  def createFromS3File(dieOnFailure: Boolean=true) {
    findS3File() match {
      case None =>
        if (dieOnFailure) {
          println("No .s3 file found; run this command and specify the awsAccountName and bucketName")
          sys.exit(-2)
        }

      case Some(file) =>
        val s3File: S3File = parseS3File(file)
        s3fileTos3Option(s3File) match {
          case None =>
            if (dieOnFailure) {
              println("No authentication credentials stored in .aws")
              sys.exit(-4)
            }

          case Some(s3) =>
            if (bucketExists(s3File.bucketName)(s3)) {
              if (dieOnFailure) {
                println("The bucket '%s' exists for AWS account %s".format(s3File.bucketName, s3File.accountName))
                sys.exit(-3)
              } else
                return
            }
            doit(s3, s3File.accountName, s3File.bucketName)
        }
    }
  }

  /**
    * Error if bucket exists else create bucket
    * if .s3 file exists in current directory or parent, modify it by setting bucketName
    * else tell user that they could run link command
    */
  def createFromCmdLine(dieOnFailure: Boolean=true): Any = {
    findS3File() match {
      case None =>
        val accountName = args(1)
        val bucketName = args(2)
        getAuthentication(accountName) match {
          case None =>
            if (dieOnFailure) {
              println("Error: Authentication credentials not found in %s; try the link subcommand".format(credentialPath))
              sys.exit(-4)
            }

          case Some(credentials) =>
            val s3 = new S3(credentials.accessKey, credentials.secretKey)
            try {
              doit(s3, accountName, bucketName)
              writeS3(S3File(accountName, bucketName, None))
            } catch {
              case ex =>
                println(ex.getMessage)
            }
        }

      case Some(file) =>
        if (dieOnFailure) {
          println("%s file found; run this command but do not specify the awsAccountName and bucketName, or move to another directory".format(file.getCanonicalPath))
          sys.exit(-2)
        }
    }
  }

  def doit(s3: S3, accountName: String, bucketName: String) {
    s3.createBucket(bucketName)
    println("Created bucket %s for AWS account %s".format(bucketName, accountName))
    println("You can access the new bucket at " + s3.getResourceUrl(bucketName, ""))
    s3.uploadString(bucketName, "index.html",
      """<h1>Hello, World!</h1>
        | <p>If there is no index.html file you will get an error when you attempt to view the web site.</p>""".stripMargin)
  }
}
