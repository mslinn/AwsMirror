/* Copyright 2012 Micronautics Research Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License. */

package com.micronautics.aws

import scalax.file.Path
import java.io.File
import com.codahale.jerkson.Json._
import io.Source
import akka.actor.ActorSystem
import scalax.io.Codec
import org.joda.time.format.DateTimeFormat
import org.codehaus.jackson.map.ObjectMapper

object Main extends App {
  def credentialPath: Path = Path(new File(sys.env("HOME"))) / ".aws"
  lazy val system = ActorSystem()
  var s3Option: Option[S3] = None
  val dtFormat = DateTimeFormat.forPattern("HH:mm:ss 'on' mmm, dd YYYY")

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

  /** @return `.aws` contents as a String */
  def credentialFileContents: Option[String] = {
    val file = new File(credentialPath.path)
    if (file.exists)
      Some(Source.fromFile(file).mkString)
    else
      None
  }

  def help: Unit = {
    println(
      """Usage: aws <action>
        |  Where <action> is one of:
        |    auth   provide authentication for an additional AWS account
        |      add accountName - you will be prompted to add credentials for AWS accountName
        |      delete - accountName delete authentication for specified AWS account name
        |      list   - list authentications
        |      modify - accountName modify authentication for specified AWS account name
        |    create [accountName bucketName]
        |        create specified bucket for accountName, or bucket specified in relevent .s3 file, enables web acces and uploads a short index.html file
        |    delete [accountName bucketName]
        |        delete specified bucket from AWS account, or bucket specified in relevent .s3 file
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

  /** @return true if bucket exists for given or implicit AWS S3 account credentials */
  def bucketExists(bucketName: String)(implicit s3: S3): Boolean = s3.listBuckets().contains(bucketName)

  def readS3File(): S3File = {
    findS3File() match {
      case None =>
        null

      case Some(file) =>
        parseS3File(file)
    }
  }

  /**
    * Walk up from the current directory
    * @return Some(File) for first `.s3` file found, or None
    */
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

  /** @return `Some(S3)` for any `.s3` file found, or None */
  def findS3: Option[S3] = findS3FileObject match {
    case None =>
      None

    case Some(s3File) =>
      s3Option(s3File.accountName)
  }

  def s3Option(accountName: String): Option[S3] = {
    getAuthentication(accountName) match {
      case None =>
        None

      case Some(credentials) =>
        Some(new S3(credentials.accessKey, credentials.secretKey))
    }
  }

  /** @return `Some(S3File)` for any `.s3` file found, or None */
  def findS3FileObject: Option[S3File] = findS3File() match {
    case None =>
      None

    case Some(file) =>
      Some(parseS3File(file))
  }

  /**
    * Parse `.aws` file if it exists
    * @return Some(Credentials) authentication for given accountName, or None
    */
  def getAuthentication(accountName: String): Option[Credentials] = {
    credentialFileContents match {
      case None =>
        None

      case Some(contents) =>
        val creds = AllCredentials(parse[Array[Credentials]](contents)).flatMap { cred =>
          if (cred.awsAccountName==accountName)
            Some(cred)
          else
            None
        }
        if (creds.length==0)
          None
        else
          Some(creds.head)
    }
  }

  implicit def s3fileTos3Option(s3File: S3File): Option[S3] = {
    getAuthentication(s3File.accountName) match {
      case None =>
        Main.s3Option = None

      case Some(credentials) =>
        val s3 = new S3(credentials.accessKey, credentials.secretKey)
        Main.s3Option = Some(s3)
    }
    Main.s3Option
  }

  /** @return S3File from parsing JSON in given `.s3` file */
  def parseS3File(file: File): S3File = try {
    parse[S3File](Path(file).slurpString(Codec.UTF8))
  } catch {
    case e =>
      println("Error parsing " + file.getAbsolutePath + ":\n" + e.getMessage)
      sys.exit(-2)
      null
  }

  /** Interactive prompt/reply */
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

  def writeS3(contents: String): Unit = {
    val s3File = new File(System.getProperty("user.dir"), ".s3")
    val s3Path = Path(s3File)
    s3Path.write(contents)
  }

  /**
   * Write `.s3` file
   * @return String indicating when last synced, if ever synced
   */
  def writeS3(newS3File: S3File): String = {
    val synced = newS3File.lastSyncOption match {
      case None =>
        "never synced"

      case Some(dateTime) =>
        "last synced " + dtFormat.print(dateTime)
    }

    writeS3(generate(newS3File) + "\n")

// Error: No suitable constructor found for type [simple type, class com.micronautics.aws.S3File]:
// can not instantiate from JSON object (need to add/enable type information?)
//    val mapper = new ObjectMapper()
//    val myObject = mapper.readValue(generate(newS3File), classOf[S3File])
//    val writer = mapper.writerWithDefaultPrettyPrinter()
//    writeS3(writer.writeValueAsString(myObject))

    synced
  }
}
