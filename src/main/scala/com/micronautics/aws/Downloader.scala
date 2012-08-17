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

import Model._
import Util._
import akka.dispatch.{ ExecutionContext, Future }
import com.amazonaws.services.s3.model.S3ObjectSummary
import java.io.{ IOException, File }
import java.util.ArrayList
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import scala.collection.mutable

/** Downloads on multiple threads if Model.multithreadingEnabled is true */
class Downloader(overwrite: Boolean) {
  private[aws] val s3 = new S3(credentials.accessKey, credentials.secretKey)
  private val futures = mutable.ListBuffer.empty[Future[Unit]] // not used at present
  private implicit val dispatcher: ExecutionContext = Main.system.dispatcher
  private val logger = LoggerFactory.getLogger(getClass)

  def download(localDir: File): ArrayList[File] = {
    val results = new ArrayList[File]()
    logger.debug("Downloading to " + localDir)
    if (!Model.s3ObjectDataFetched) {
      Model.allNodes = s3.getAllObjectData(bucketName, null) // get every object
      Model.s3ObjectDataFetched = true
    }
    Model.allNodes foreach { node: S3ObjectSummary =>
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
          if (file.getParent!=null && overwriteExisting)
            file.getParentFile.mkdirs
          if (!outFileName.endsWith("$folder$")) {
            compareS3FileAge(file, node) match {
              case r: Int if r==s3FileDoesNotExist =>
                logger.debug("Remote copy of '%s' does not exist, so it was not downloaded.".format(file.getAbsolutePath))

              case r: Int if r==s3FileIsOlderThanLocal =>
                logger.debug("Remote copy of '%s' is older (%s) than the\n  local copy at '%s' (%s), so it was not downloaded.".
                  format(node.getKey, dtFmt(node.getLastModified), file.getAbsolutePath, dtFmt(file.lastModified)))

              case r: Int if r==s3FileSameAgeAsLocal =>
                if (overwriteExisting) {
                  logger.debug("Downloading because the remote copy of %s is the same age as the local copy and overwrite is enabled.".format(file.getAbsolutePath))
                  if (multithreadingEnabled)
                    futures += Future(downloadOne(localDir, node, file))
                  else
                    downloadOne(localDir, node, file)
                } else
                  logger.debug("Remote copy of %s is the same age as the local copy and overwrite is disabled, so it was not downloaded.".format(file.getAbsolutePath))

              case r: Int if r==s3FileNewerThanLocal =>
                logger.debug("Downloading '%s' (%s) to '%s' (%s) because the remote copy is newer.".
                  format(node.getKey, dtFmt(node.getLastModified), fileNamePrefix(localDir, file), dtFmt(file.lastModified)))
                if (multithreadingEnabled)
                  futures += Future(downloadOne(localDir, node, file))
                else
                  downloadOne(localDir, node, file)

              case r: Int if r==s3FileDoesNotExistLocally =>
                logger.debug("Downloading '%s' because '%s' does not exist locally.".format(node.getKey, file.getAbsolutePath))
                if (multithreadingEnabled)
                  futures += Future(downloadOne(localDir, node, file))
                else
                  downloadOne(localDir, node, file)
            }
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

  def fileNamePrefix(base: File, file: File): String = {
    val basePath = base.getAbsolutePath
    val path = file.getAbsolutePath
    path.substring(0, basePath.length)
  }

  def downloadOne(localDir: File, node: S3ObjectSummary, outFile: File): Unit = {
    FileUtils.copyInputStreamToFile(s3.downloadFile(bucketName, node.getKey), outFile)
    outFile.setLastModified(node.getLastModified().getTime)
    logger.info("Downloaded '%s' (last modified %s, %d bytes).".
      format(relativeFileName(localDir, outFile), node.getKey, dtFmt(node.getLastModified)))
    if (node.getSize!=outFile.length())
      logger.error("Error: %s has different length (%d) than remote version (%d).".
        format(outFile.getAbsolutePath, node.getSize))
    if (node.getLastModified!=outFile.lastModified())
      logger.error("Error: %s has last modified date (%s) than remote version (%s).".
        format(outFile.getAbsolutePath, dtFmt(outFile.lastModified), dtFmt(node.getLastModified)))
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
