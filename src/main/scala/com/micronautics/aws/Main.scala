package com.micronautics.aws

import scalax.file.Path
import java.io.File
import com.codahale.jerkson.Json._
import io.Source
import akka.actor.ActorSystem

object Main extends App {
  def credentialPath: Path = Path(new File(sys.env("HOME"))) / ".aws"
  implicit val system = ActorSystem()

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

      case "download" =>
        new Download(args)

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
        |    auth   provide authentication for an additional AWS account
        |      delete - accountName delete authentication for specified AWS account name
        |      list   - list authentications
        |      modify - accountName modify authentication for specified AWS account name
        |    create [accountName bucketName]
        |        create specified bucket for accountName, or bucket specified in relevent .s3 file
        |    delete [bucketName]
        |        delete specified bucket, or bucket specified in relevent .s3 file
        |    download
        |      download bucket specified in relevent .s3 file to the entire tree
        |    empty [bucketName]
        |      empty specified bucket, or bucket specified in relevent .s3 file
        |    help    print this message and exit
        |    link [accountName bucketName]
        |      If accountName and bucketName are not specified, display contents of .s3 file in current directory or a parent directory.
        |      Otherwise create or modify .s3 file in current directory by setting accountName and bucketName
        |    sync    sync directory tree to specified bucket
        |    upload  upload entire directory tree to bucket specified in relevent .s3 file
      """.stripMargin)
    System.exit(0)
  }

  def findS3File(fileName: String=".s3", directory: File=new File(System.getProperty("user.dir"))): Option[File] = {
    val file = new File(directory, fileName)
    if (file.exists()) {
      Some(file)
    } else {
      val parent = directory.getParent
      if (parent==null)
        None
      else
        findS3File(fileName, new File(parent))
    }
  }

  def fileContents = Source.fromFile(credentialPath.path).mkString

  def getAuthentication(accountName: String): Option[Credentials] = {
    if (credentialPath.exists) {
      val creds = AllCredentials(parse[Array[Credentials]](fileContents)).flatMap { cred =>
        if (cred.awsAccountName==accountName)
          Some(cred)
        else
          None
      }
      if (creds.length==0)
        None
      else
        Some(creds.head)
    } else
      None
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
