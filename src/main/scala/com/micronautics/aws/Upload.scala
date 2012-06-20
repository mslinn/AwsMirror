package com.micronautics.aws

import com.micronautics.aws.Main._
import com.codahale.jerkson.Json._
import scala.Some
import scalax.file.Path
import scalax.io.Codec
import java.nio.file.Paths

class Upload(args: Array[String]) {
  if (!credentialPath.exists) {
    println(".aws file not found in %s\nUse 'auth add' subcommand to create".format(credentialPath.path))
    System.exit(-1)
  }

  args.length match {
    case 1 =>
    // Upload entire tree to bucket specified in .s3 file in this directory or parent;
    // Error if no .aws or .s3 file or if bucket does not exist
    findS3File() match {
      case None =>
        println("Error: This directory is not linked with an AWS S3 bucket.\nUse the link subcommand to establish the link.")

      case Some(file) =>
        val s3File = parse[S3File](Path(file).slurpString(Codec.UTF8))
        getAuthentication(s3File.accountName) match {
          case None =>
            println("Error: %s was not found".format(file.getCanonicalPath))
            System.exit(-1)

          case Some(credentials) =>
            val s3 = new S3(credentials.accessKey, credentials.secretKey)
            if (!s3.listBuckets().contains(s3File.bucketName)) {
              println("Error: AWS account %s does not define bucket %s".format(s3File.accountName, s3File.bucketName))
              System.exit(-1)
            }
            new Uploader(credentials, s3File.bucketName).upload(file.getParentFile)

            // continue uploading until Control-C
            val watchPath = Paths.get(file.getParent)
            new DirectoryWatcher(watchPath).watch()
        }
    }

    case _ =>
      println("Error: Too many arguments provided for upload")
      help
  }
}
