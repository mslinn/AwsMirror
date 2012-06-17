package com.micronautics.aws

import collection.mutable.MutableList
import scala.Array

case class Credentials(awsAccountName: String, accessKey: String, secretKey: String)

class AllCredentials extends MutableList[Credentials] {
  def addAll(credArray: Array[Credentials]): AllCredentials = {
    credArray foreach { credentials => this += credentials }
    this
  }

  def defines(accountName: String): Boolean = groupBy(_.awsAccountName).keySet.contains(accountName)

  //def find(accountName: String): Option[Credentials] = groupBy(_.awsAccountName).get(accountName)
}

object AllCredentials {
  def apply(credArray: Array[Credentials]): AllCredentials = {
    val allCredentials = new AllCredentials()
    allCredentials.addAll(credArray)
  }
}

object AWS {
  var allCredentials = new AllCredentials()
}

object AuthAction extends Enumeration {
   type AuthAction = Value
   val add, delete, list, modify = Value
 }
