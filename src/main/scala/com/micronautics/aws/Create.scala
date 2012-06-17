package com.micronautics.aws

import com.micronautics.aws.Main._

class Create(args: Array[String]) {
  args.length match {
    case 1 =>
      // todo create bucket specified in .s3 file in this directory or parent; error if no .s3 file
      // todo error if bucket exists

    case 2 =>
      // todo error if bucket exists
      // todo else create bucket
      // todo if .s3 file exists in current directory or parent, modify it by setting bucketName
      // todo else tell user that they could run link command

    case _ =>
      println("Error: Too many arguments provided for link")
      help
  }

}
