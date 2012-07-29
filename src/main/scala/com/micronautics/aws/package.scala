package com.micronautics.aws

import collection.mutable.MutableList
import scala.Array
import org.joda.time.DateTime
import java.util.regex.Pattern
import java.util.{Arrays, ArrayList}
import org.codehaus.jackson.annotate.JsonIgnore
import collection.JavaConversions._

case class Credentials(awsAccountName: String, accessKey: String, secretKey: String)

class AllCredentials extends MutableList[Credentials] {
  def addAll(credArray: Array[Credentials]): AllCredentials = {
    credArray foreach { credentials => this += credentials }
    this
  }

  def defines(accountName: String): Boolean = groupBy(_.awsAccountName).keySet.contains(accountName)
}

object AllCredentials {
  def apply(credArray: Array[Credentials]): AllCredentials = {
    val allCredentials = new AllCredentials()
    allCredentials.addAll(credArray)
  }
}

object AWS {
  val defaultIgnores = Seq(".*.tmp", ".git.*", "cvs", ".svn", ".*~")
  var allCredentials = new AllCredentials()
}

object AuthAction extends Enumeration {
   type AuthAction = Value
   val add, delete, download, list, modify = Value
 }

case class S3File(accountName: String, bucketName: String, lastSyncOption: Option[DateTime]=None, ignores: Seq[String]=AWS.defaultIgnores) {
  @JsonIgnore val ignoredRegexes: Seq[Pattern] = ignores.map { x => Pattern.compile(x) }
}