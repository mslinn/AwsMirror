package com.micronautics.aws

import com.micronautics.aws.Main._

/** Delete S3 bucket */
class Delete(args: Array[String]) {
  if (!credentialPath.exists) {
    println(".aws file not found in %s\nUse 'auth add' subcommand to create".format(credentialPath.path))
    System.exit(-1)
  }

  args.length match {
    case 1 => // delete
      // Delete bucket specified in .s3 file in this directory or parent; error if no .s3 file
      // Error if bucket does not exist
      // Does not delete .s3 file, in case user wants to recreate the bucket
      findS3FileObject match {
        case None =>
          println("No .s3 file found in this directory or its parents")
          System.exit(0)

        case Some(s3File) =>
          s3Option(s3File.accountName) match {
            case None =>
              println("AWS credentials did not match for AWS account '%s' and bucket '%s'".format(s3File.accountName, s3File.bucketName))

            case Some(s3) =>
              s3.deleteBucket(s3File.bucketName)
              println("AWS bucket '%s' deleted from account '%s'. %s was not deleted in case you want to recreate the bucket easily with the create subcommand.".
                format(s3File.bucketName, s3File.accountName, findS3File().get.getCanonicalPath))
          }
      }

    case 3 => // delete awsAccountName bucketName
      // todo error if bucket does not exist
      // todo else delete bucket
      // todo if .s3 file exists in current directory or parent, tell user that they could run create then sync commands

    case _ =>
      println("Error: Too many arguments provided for delete")
      help
  }

}
