package com.micronautics.aws

import scalax.file.Path
import java.io.File

object Main extends App {
  def credentialPath: Path = Path(new File(sys.env("HOME"))) / ".aws"

  override def main(args: Array[String]) {
    if (args.length==0)
      help

    args(0).toLowerCase match {
      case "auth" =>
        new Auth(args)

      case "create" =>
        new Create(args)

      case "delete" =>
        new Delete(args)

      case "empty" =>
        new Empty(args)

      case "help" =>
        help

      case "link" =>
        new Link(args)

      case "sync" =>
        new Sync(args)

      case "upload" =>
        new Upload(args)

      case wtf =>
        println("%s is an unrecognized command".format(wtf))
        help
    }
  }

  def help: Unit = {
    println(
      """Usage: aws <action>
        |  Where <action> is one of:
        |    auth                  provide authentication for an additional AWS account
        |                          delete - accountName delete authentication for specified AWS account name
        |                          list   - list authentications
        |                          modify - accountName modify authentication for specified AWS account name
        |    create [bucketName]   create specified bucket, or bucket specified in relevent .s3 file
        |    delete [bucketName]   delete specified bucket, or bucket specified in relevent .s3 file
        |    empty [bucketName]    empty specified bucket, or bucket specified in relevent .s3 file
        |    help                  print this message and exit
        |    link [bucketName]     If bucketName is not specified, display contents of .s3 file in current directory or a parent directory.
        |                          Otherwise create or modify .s3 file in current directory by setting bucketName
        |    sync                  sync directory tree to specified bucket
        |    upload                upload directory tree to bucket specified in relevent .s3 file
        |                          -d delete files on AWS that are not in the local directory, after files are uploaded
      """.stripMargin)
    System.exit(0)
  }

  def prompt(msg: String, defaultValue: String=null): String = {
    print(msg +
      (if (defaultValue!=null)
        " <%s>: ".format(defaultValue)
      else
        ": "))
    val userInput = Console.readLine()
    if (userInput=="" && defaultValue!=null)
      defaultValue
    else
      userInput
  }
}
