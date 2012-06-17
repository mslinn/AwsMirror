package com.micronautics.aws

import com.micronautics.aws.Main._

class Delete(args: Array[String]) {
  args.length match {
    case 1 =>
    // todo delete bucket specified in .s3 file in this directory or parent; error if no .s3 file
    // todo error if bucket does not exist

    case 2 =>
    // todo error if bucket does not exist
    // todo else delete bucket
    // todo if .s3 file exists in current directory or parent, tell user that they could run create then sync commands

    case _ =>
      println("Error: Too many arguments provided for delete")
      help
  }

}
