package com.micronautics.aws

import com.micronautics.aws.Main._
import java.nio.file.Paths

class Sync(args: Array[String]) {
  if (!credentialPath.exists) {
    println(".aws file not found in %s\nUse 'auth add' subcommand to create".format(credentialPath.path))
    System.exit(-1)
  }

  args.length match {
    case 1 =>
    // todo sync bucket specified in .s3 file in this directory or parent; error if no .s3 file
    // todo error if bucket does not exist

    // continue uploading until Control-C
//    val watchPath = Paths.get(file.getParent)
//    new DirectoryWatcher(watchPath).watch()

    case 2 =>
    // todo error if bucket does not exist
    // todo else sync bucket

    // continue uploading until Control-C
//    val watchPath = Paths.get(file.getParent)
//    new DirectoryWatcher(watchPath).watch()

    case _ =>
      println("Error: Too many arguments provided for sync")
      help
  }
}
