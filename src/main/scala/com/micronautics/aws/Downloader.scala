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

import com.amazonaws.services.s3.model.S3ObjectSummary
import java.io.{IOException, File}
import java.util.{Date, ArrayList}
import scala.collection.JavaConversions._
import org.apache.commons.io.FileUtils
import akka.dispatch.{ExecutionContext, Await, Future}
import collection.mutable
import java.text.SimpleDateFormat
import org.slf4j.LoggerFactory

/** Downloads on multiple threads */
class Downloader(credentials: Credentials, bucketName: String, overwrite: Boolean) {
  private val logger = LoggerFactory.getLogger(getClass)
  private[aws] val s3 = new S3(credentials.accessKey, credentials.secretKey)
  private val futures = mutable.ListBuffer.empty[Future[Boolean]]
  private implicit val dispatcher: ExecutionContext = Main.system.dispatcher

  def download(localDir: File): ArrayList[File] = {
    val results = new ArrayList[File]()
    val allNodes = s3.getAllObjectData(bucketName, null) // get every object
    allNodes foreach { node: S3ObjectSummary =>
      val outFileName: String = if (node.getKey.startsWith("/") || node.getKey.startsWith("\\"))
                node.getKey.substring(1) else node.getKey
      val outFile: File = new File(outFileName)
      results.add(outFile)
      try {
        if (node.getKey.endsWith("/")) {
          if (!outFile.exists) {
            logger.debug("Making " + relativeFileName(localDir, outFile))
            outFile.mkdirs
          }
        } else {
          val file = new File(localDir, outFileName)
          val overwriteExisting: Boolean = !(file.exists()) || overwrite
          if (outFile.getParent!=null && overwriteExisting)
            outFile.getParentFile.mkdirs
          val s3FileNewer = s3FileIsNewer(file, node)
          if (!outFileName.endsWith("$folder$")) {
            if (overwriteExisting  && s3FileNewer)
              futures += Future(downloadOne(outFile, node))
            else
              logger.debug("Remote copy of %s is not newer, so it was not downloaded".format(outFileName))
          }
        }
      } catch {
        case ioe: IOException =>
          println(ioe)
      }
    }
    results
  }

  def relativeFileName(base: File, file: File): String = {
    val basePath = base.getAbsolutePath
    val path = file.getAbsolutePath
    path.substring(basePath.length+1)
  }

  def s3FileIsNewer(file: File, node: S3ObjectSummary): Boolean = {
    if (!file.exists) return true
    val s3NodeLastModified: Date = node.getLastModified
    val isNewer: Boolean = s3NodeLastModified.getTime > file.lastModified
//    logger.debug("s3NodeLastModified.getTime()=" + s3NodeLastModified.getTime +
//      "; file.lastModified()=" + file.lastModified + "; newer=" + isNewer)
    return isNewer
  }

  private val formatter = new SimpleDateFormat("yyyy-MM-dd 'at' hh:mm:ss z")

  def downloadOne(outFile: File, node: S3ObjectSummary) = {
    logger.info("Downloading " + outFile + ", last modified " + formatter.format(node.getLastModified()) + ", " + node.getSize + " bytes.")
    FileUtils.copyInputStreamToFile(s3.downloadFile(bucketName, node.getKey), outFile)
    outFile.setLastModified(node.getLastModified().getTime)
  }

  def deleteBadKeys: Unit = {
    val allNodes = s3.getAllObjectData(bucketName, "") // get every object
    allNodes foreach { node: S3ObjectSummary =>
      val nodeKey = node.getKey()
      if (!nodeKey.startsWith("/"))
        s3.deleteObject(bucketName, nodeKey)
    }
  }
}
