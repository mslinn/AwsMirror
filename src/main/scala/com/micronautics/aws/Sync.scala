package com.micronautics.aws

import com.micronautics.aws.Main._

class Sync(args: Array[String]) {
  args.length match {
    case 1 =>
    // todo sync bucket specified in .s3 file in this directory or parent; error if no .s3 file
    // todo error if bucket does not exist

    case 2 =>
    // todo error if bucket does not exist
    // todo else sync bucket

    case _ =>
      println("Error: Too many arguments provided for sync")
      help
  }

}
