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
import Upload._
import akka.dispatch.Future
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._

/** Uploads on (at least) one thread, downloads on multiple threads */
class Sync(args: Array[String]) {
  private val logger = LoggerFactory.getLogger(getClass)
  if (!credentialPath.exists) {
    println(".aws file not found in %s\nUse 'auth add' subcommand to create".format(credentialPath.path))
    System.exit(-1)
  }

  args.length match {
    case 1 => // sync
      // Sync bucket specified in .s3 file in this directory or parent
      // Continue uploading until Control-C
      // Error if no .s3 file or bucket does not exist

      val (credentials, s3fileObject, s3File) = retrieveParams
      Model.bucketName = s3fileObject.bucketName
      Model.credentials = credentials
      Model.ignoredPatterns = s3fileObject.ignoredPatterns
      Model.s3 = s3fileObject.get
      new Downloader(false).download(s3File.getParentFile)
      Future(upload(s3File, false))(Main.system.dispatcher)

    case _ =>
      println("Error: Too many arguments provided for sync")
      help
  }
}
