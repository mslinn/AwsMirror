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
          getS3(s3File) match {
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
      // todo error if bucket exists
      // todo else create bucket
      // todo if .s3 file exists in current directory or parent, modify it by setting bucketName
      // todo else tell user that they could run link command

    case _ =>
      println("Error: Too many arguments provided for link")
      help
  }

}
