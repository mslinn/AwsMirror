package com.micronautics.aws

import com.micronautics.aws.Main._

class Empty(args: Array[String]) {
  args.length match {
    case 1 =>
    // todo empty bucket specified in .s3 file in this directory or parent; error if no .s3 file
    // todo error if bucket does not exist

    case 2 =>
    // todo error if bucket does not exist
    // todo else empty bucket
    // todo if .s3 file exists in current directory or parent, tell user that they could run sync or upload commands

    case _ =>
      println("Error: Too many arguments provided for empty")
      help
  }

}
