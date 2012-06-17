package com.micronautics.aws

import com.micronautics.aws.Main._

class Link(args: Array[String]) {
  args.length match {
    case 1 =>
      // todo display .s3 file in this directory or parent

    case 2 =>
      // todo  create or modify .s3 file in current directory by setting bucketName

    case _ =>
      println("Error: Too many arguments provided for link")
      help
  }

}
