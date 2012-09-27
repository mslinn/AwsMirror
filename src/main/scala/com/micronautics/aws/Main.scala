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

import akka.actor.ActorSystem
import ch.qos.logback.classic.{Level, Logger}
import com.amazonaws.services.s3.model.S3ObjectSummary
import java.io.File
import org.slf4j.LoggerFactory
import org.slf4j.Logger.ROOT_LOGGER_NAME
import collection.JavaConversions._

object Main extends App {
  lazy val system = ActorSystem()

  override def main(args: Array[String]) {
    if (args.length==0)
      help
    process(args.toList)
  }

  private lazy val logger = LoggerFactory.getLogger(ROOT_LOGGER_NAME).asInstanceOf[Logger]
  private lazy val logLevels = Vector[Level](Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG)
  private var levelWasSet = false
  private var multithreadingWasSet = false

  protected[aws] def increaseLogLevel: Unit = {
    val logLevel = logger.getLevel
    val index = math.min(logLevels.size-1, logLevels.indexOf(logLevel)+1)
    logger.setLevel(logLevels(index))
    levelWasSet = true
  }

  protected[aws] def decreaseLogLevel: Unit = {
    val logLevel = logger.getLevel
    val index = math.max(0, logLevels.indexOf(logLevel)-1)
    logger.setLevel(logLevels(index))
    levelWasSet = true
  }

  private[aws] def process(args: List[String]): Boolean = {
    args match {
      case "-v" :: rest =>
        decreaseLogLevel
        process(rest)

      case "-V" :: rest =>
        increaseLogLevel
        process(rest)

      case "-M" :: rest =>
        Model.multithreadingEnabled = true
        multithreadingWasSet = true
        process(rest)

      case "-m" :: rest =>
        Model.multithreadingEnabled = false
        multithreadingWasSet = true
        process(rest)

      case mandatoryArgs =>
        if (levelWasSet)
          println("Log level is %s.".format(logger.getLevel))
        if (multithreadingWasSet)
          println("Multithreading is %s.".format(if (Model.multithreadingEnabled) "enabled" else "disabled"))
        commands(mandatoryArgs)
        true
    }
  }

  private[aws] def commands(args: List[String]): Unit = {
    args match {
      case "auth" :: rest =>
        new Auth(rest.toArray)

      case "create" :: rest =>
        new Create(rest.toArray)

      case "delete" :: rest =>
        new Delete(rest.toArray)

      case ("download" | "down") :: rest =>
        new Download(rest.toArray)

      case "empty" :: rest =>
        new Empty(rest.toArray)

      case "help" :: rest =>
        help

      case "link" :: rest =>
        new Link(rest.toArray)

      case "sync" :: rest =>
        new Sync(rest.toArray)

      case ("upload" | "up") :: rest =>
        new Upload(rest.toArray)

      case wtf =>
        println("%s is an unrecognized command".format(wtf))
        help
    }
  }

  def help: Unit = {
    println(
      """AwsMirror v0.1.0-SNAPSHOT
        |Usage: aws <option> <action>
        |  Where <option> is one of:
        |      -m    multithreading enabled
        |      -M    multithreading disabled
        |      -v    less verbose output
        |      -V    more verbose output
        |  and <action> is one of:
        |    auth   provide authentication for an additional AWS account
        |      add accountName      you will be prompted to add credentials for AWS accountName
        |      delete accountName   delete authentication for specified AWS account name
        |      list                 list authentications
        |      modify accountName   modify authentication for specified AWS account name
        |    create [accountName bucketName]
        |        create specified bucket for accountName, or bucket specified in relevent .s3 file, enables web access and uploads a short index.html file
        |    delete [accountName bucketName]
        |        delete specified bucket from AWS account, or bucket specified in relevent .s3 file
        |    download, down
        |      download bucket specified in relevent .s3 file to the entire tree
        |    empty [bucketName]
        |      empty specified bucket, or bucket specified in relevent .s3 file
        |    help    print this message and exit
        |    link [accountName bucketName]
        |      If accountName and bucketName are not specified, display contents of .s3 file in current directory or a parent directory.
        |      Otherwise create or modify .s3 file in current directory by setting accountName and bucketName
        |    sync    sync directory tree to specified bucket; continues monitoring directory tree and uploads changes
        |    upload, up   upload to bucket specified in relevent .s3 file; continues monitoring directory tree and uploads changes
        |    upload, up (file, directory or entire directory tree)   uploads file or directory and exits
        |      """.stripMargin)
    sys.exit(0)
  }

  /** @return true if bucket exists for given or implicit AWS S3 account credentials */
  def bucketExists(bucketName: String)(implicit s3: S3): Boolean = s3.bucketExists(bucketName)

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

  /** @return -2 if s3File does not exist or if there is a read error,
     *          -1 if s3File is older than local copy,
     *           0 if same age as local copy,
     *           1 if remote copy is newer,
     *           2 if local copy does not exist */
  def compareS3FileAge(file: File, path: String): Int = {
    Model.allNodes.foreach { (node: S3ObjectSummary) =>
      val key: String = node.getKey
      try {
        if (key.compareTo(path) == 0)
          return Util.compareS3FileAge(file, node)
      } catch {
        case e: Exception =>
          System.out.println(e.getMessage() + ": " + key)
          return S3Model.s3FileDoesNotExist
      }
    }
    S3Model.s3FileDoesNotExist
  }
}
