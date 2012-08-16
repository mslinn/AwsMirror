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

import com.micronautics.aws.AuthAction._
import com.codahale.jerkson.Json._
import scala.Some
import Main._

class Auth(args: Array[String]) {
  args.length match {
    case 0 =>
      println("Warning: No authentication action specified")
      help

    case 1 =>
      try {
        auth(AuthAction.withName(args(0).toLowerCase))
      } catch {
        case nsee: NoSuchElementException =>
          println("Error: '%s' is not a valid option for the auth command".format(args(0)))
         help
      }

    case 2 =>
      try {
        auth(AuthAction.withName(args(0).toLowerCase()), args(1))
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
      case `add` =>
        if (accountName==null) {
          println("Error: No AWS account name was specified.")
          help
        }

        val credentials: AllCredentials = new AllCredentials()
        credentialFileContents match {
          case None =>

          case Some(contents) =>
            credentials.addAll(parse[Array[Credentials]](contents))
            if (credentials.defines(accountName)) {
              println("Error: AWS account '%s' already defines in %s".format(accountName, credentialPath.path))
              System.exit(-1)
            }
        }
        val accessKey = prompt("Access key for AWS account " + accountName)
        val secretKey = prompt("Secret key for AWS account " + accountName)
        credentials += Credentials(accountName, accessKey, secretKey)

        println(listBuckets(accessKey, secretKey, accountName))

        val json = generate(credentials)
        credentialPath.write(json + "\n")

      case `delete` =>
        if (accountName==null) {
          println("Error: No AWS account name was specified.")
          help
        }
        credentialFileContents match {
          case None =>
            println(".aws file not found in %s".format(credentialPath.path))
            System.exit(-1)

          case Some(contents) =>
            val oldCredentials = AllCredentials(parse[Array[Credentials]](contents))
            if (!oldCredentials.defines(accountName)) {
              println("Error: authorization information for AWS account '%s' is not present in %s".format(accountName, credentialPath.path))
              System.exit(-1)
            }
            val newCredentials = oldCredentials.flatMap { cred =>
              if (cred.awsAccountName==accountName)
                None
              else
                Some(cred)
            }
            val prettyPrintedCredentials = generate(newCredentials).replaceAll("(.*?:(\\[.*?\\],|.*?,))", "$0\n ")
            credentialPath.write(prettyPrintedCredentials)
        }

      case `list` =>
        credentialFileContents match {
          case None =>
            println(".aws file not found in %s".format(credentialPath.path))
            System.exit(-1)

          case Some(contents) =>
            println("List of AWS credentials in %s".format(credentialPath.path))
            parse[Array[Credentials]](contents) foreach { creds: Credentials =>
              println("""  AWS account name: %s
                        |    Access key: %s
                        |    Secret key: %s
                        |    %s""".stripMargin.
                format(creds.awsAccountName, creds.accessKey, creds.secretKey,
                       listBuckets(creds.accessKey, creds.secretKey, creds.awsAccountName)))
            }

      case `modify` =>
        if (accountName=="") {
          println("Error: No AWS account name specified on command line")
          return
        }
        credentialFileContents match {
          case None =>
            println(".aws file not found in %s".format(credentialPath.path))
            System.exit(-1)

          case Some(contents) =>
            val oldCredentials = parse[Array[Credentials]](contents)
            val newCredentials = oldCredentials map { credentials =>
              if (credentials.awsAccountName==accountName) {
                val awsAccountName = prompt("AWS account name", credentials.awsAccountName)
                val accessKey = prompt("Access key for AWS account " + awsAccountName, credentials.accessKey)
                val secretKey = prompt("Secret key for AWS account " + awsAccountName, credentials.secretKey)
                Credentials(awsAccountName, accessKey, secretKey)
              } else {
                credentials
              }
            }
            credentialPath.write(generate(newCredentials))
          }
      }
    }
  }

  def listBuckets(accessKey: String, secretKey: String, accountName: String): String = {
    val s3 = new S3(accessKey, secretKey)
    Main.s3Option = Some(s3)
    val buckets = s3.listBuckets()
    if (buckets.length == 0) {
      "Account %s has no buckets".format(accountName)
    } else {
      "Account %s has the following buckets: ".format(accountName) + buckets.mkString(", ")
    }
  }
}
