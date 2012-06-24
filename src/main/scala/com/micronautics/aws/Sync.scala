package com.micronautics.aws

import com.micronautics.aws.Main._
import Upload._
import akka.dispatch.Future

/** Uploads on (at least) one thread, downloads on multiple threads */
class Sync(args: Array[String]) {
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
      Future(upload(credentials, s3fileObject.bucketName, s3File))(Main.system.dispatcher)
      new Downloader(credentials, s3fileObject.bucketName, false).download(s3File.getParentFile)

    case _ =>
      println("Error: Too many arguments provided for sync")
      help
  }
}
