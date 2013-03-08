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

import AuthAction._
import Main._
import Util._
import com.codahale.jerkson.Json._
import org.slf4j.LoggerFactory
import java.io.FileWriter

class Auth(args: Array[String]) {
  private val logger = LoggerFactory.getLogger(getClass)
  args.length match {
    case 0 =>
      println("Warning: No authentication action specified")
      help

    case 1 =>
      try {
        auth(AuthAction.withName(args(0)))
      } catch {
        case nsee: NoSuchElementException =>
          println("Error: '%s' is not a valid option for the auth command".format(args(0)))
         help
      }

    case 2 =>
      try {
        auth(AuthAction.withName(args(0)), args(1))
      } catch {
        case nsee: NoSuchElementException =>
          println("Error: '%s' is not a valid option for the auth command".format(args(0)))
          help
      }

    case _ =>
      println("Error: Too many arguments provided for authentication")
      help
  }

  def auth(action: AuthAction, accountName: String=null): Unit = {
    action match {
      case AuthAction.add =>
        authAdd(accountName)

      case AuthAction.delete =>
        authDelete(accountName)

      case AuthAction.list =>
        authList

      case AuthAction.modify =>
        authModify(accountName)
    }
  }

  def authAdd(accountName: String) {
    if (accountName == null) {
      println("Error: No AWS account name was specified.")
      help
    }

    val credentials: AllCredentials = new AllCredentials()
    credentialFileContents match {
      case None =>
        logger.debug("No .aws file found, will create")

      case Some(contents) =>
        credentials.addAll(parse[Array[Credentials]](contents))
        if (credentials.defines(accountName)) {
          println("Error: AWS account '%s' already defined in %s".format(accountName, credentialPath.toAbsolutePath))
          sys.exit(-1)
        }
    }
    val accessKey = prompt("Access key for AWS account " + accountName)
    val secretKey = prompt("Secret key for AWS account " + accountName)
    credentials += Credentials(accountName, accessKey, secretKey)

    println(listBuckets(accessKey, secretKey, accountName))
    writeCredentials(credentials)
  }

  def authDelete(accountName: String) {
    if (accountName == null) {
      println("Error: No AWS account name was specified.")
      help
    }
    credentialFileContents match {
      case None =>
        println(".aws file not found in %s".format(credentialPath.toAbsolutePath))
        sys.exit(-1)

      case Some(contents) =>
        val oldCredentials = AllCredentials(parse[Array[Credentials]](contents))
        if (!oldCredentials.defines(accountName)) {
          println("Error: authorization information for AWS account '%s' is not present in %s".format(accountName, credentialPath.toAbsolutePath))
          sys.exit(-1)
        }
        val newCredentials = oldCredentials.flatMap {
          cred =>
            if (cred.awsAccountName == accountName)
              None
            else
              Some(cred)
        }.asInstanceOf[AllCredentials]
        writeCredentials(newCredentials)
    }
  }

  def authList: Unit = {
    credentialFileContents match {
      case None =>
        println(".aws file not found in %s".format(credentialPath.toAbsolutePath))
        sys.exit(-1)

      case Some(contents) =>
        println("List of AWS credentials in %s".format(credentialPath.toAbsolutePath))
        parse[Array[Credentials]](contents) foreach {
          creds: Credentials =>
            println( """  AWS account name: %s
                       |    Access key: %s
                       |    Secret key: %s
                       |    %s""".stripMargin.
              format(creds.awsAccountName, creds.accessKey, creds.secretKey,
              listBuckets(creds.accessKey, creds.secretKey, creds.awsAccountName)))
        }
    }
  }

  def authModify(accountName: String): Unit = {
    if (accountName == null || accountName == "") {
      println("Error: No AWS account name specified on command line")
      return
    }
    credentialFileContents match {
      case None =>
        println(".aws file not found in %s".format(credentialPath.toAbsolutePath))
        sys.exit(-1)

      case Some(contents) =>
        val oldCredentials = parse[Array[Credentials]](contents)
        var foundOne = false
        val newCreds = oldCredentials map {
          (credentials: Credentials) =>
            if (credentials.awsAccountName == accountName) {
              foundOne = true
              val awsAccountName = prompt("AWS account name", credentials.awsAccountName)
              val accessKey = prompt("Access key for AWS account " + awsAccountName, credentials.accessKey)
              val secretKey = prompt("Secret key for AWS account " + awsAccountName, credentials.secretKey)
              Credentials(awsAccountName, accessKey, secretKey)
            } else {
              credentials
            }
        }
        val newCredentials = new AllCredentials().addAll(newCreds)
        if (foundOne)
          writeCredentials(newCredentials)
        else
          println("You have not defined an AWS account called '%s'. Use the list subcommand to see your defined accounts.".format(accountName))
    }
  }

  def writeCredentials(credentials: AllCredentials) {
    val generatedCredentials = generate(credentials)
    val prettyPrintedCredentials = generatedCredentials.replaceAll("(.*?:(\\[.*?\\],|.*?,))", "$0\n ") + "\n"
    writeFile(credentialPath.toFile)(prettyPrintedCredentials)
    makeFileHiddenIfDos(credentialPath)
  }

  def listBuckets(accessKey: String, secretKey: String, accountName: String): String = {
    val s3 = new S3(accessKey, secretKey)
    Util.s3Option = Some(s3)
    val buckets = s3.listBuckets()
    if (buckets.length == 0) {
      "Account %s has no buckets".format(accountName)
    } else {
      "Account %s has the following buckets: ".format(accountName) + buckets.mkString(", ")
    }
  }
}
