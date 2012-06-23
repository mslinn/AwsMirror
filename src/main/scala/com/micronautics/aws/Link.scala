package com.micronautics.aws

import com.micronautics.aws.Main._
import java.io.File
import com.codahale.jerkson.Json._
import scalax.io.{Resource, Codec}
import scala.Some
import org.joda.time.format.DateTimeFormat
import scalax.file.Path

class Link(args: Array[String]) {
  if (!credentialPath.exists) {
    println(".aws file not found in %s\nUse 'auth add' subcommand to create".format(credentialPath.path))
    System.exit(-1)
  }

  private val dtFormat = DateTimeFormat.forPattern("HH:mm:ss 'on' mmm, dd YYYY")

  args.length match {
    case 1 =>
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

    case 2 =>
      println("An account name and a bucket name must both be specified.")
      help

    case 3 =>
      // Modify .s3 file in current directory or parent by setting accountName and bucketName, or create in current directory
      findS3File() match {
        case None => // create .s3 file in current directory
          val newS3File = S3File(args(1), args(2), None) // set to never synched
          val synched = newS3File.lastSyncOption match {
            case None =>
              "never synched"

            case Some(dateTime) =>
              "last synched " + dtFormat.print(dateTime)
          }
          val s3File = new File(System.getProperty("user.dir"), ".s3")
          val s3Path = Path(s3File)
          s3Path.write(generate(newS3File))
          println("%s is now linked with AWS account '%s', bucket '%s' (%s)".
            format(s3Path.toAbsolute, newS3File.accountName, newS3File.bucketName, synched))

        case Some(file) =>
          val oldS3File: S3File = parseS3File(file) //parse[S3File](Path(file).slurpString(Codec.UTF8))
          val newS3File: S3File = oldS3File.copy(args(1), args(2)) // set to never synched because we cannot know if it ever was synched previously
          val synched = newS3File.lastSyncOption match {
            case None =>
              "never synched"

            case Some(dateTime) =>
              "last synched " + dtFormat.print(dateTime)
          }
          println("%s is now linked with account '%s', bucket '%s' (%s)".
            format(file.getCanonicalPath, newS3File.accountName, newS3File.bucketName, synched))
      }


    case _ =>
      println("Error: Too many arguments provided for link")
      help
  }
}
