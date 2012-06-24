package com.micronautics.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import java.io.{IOException, File}
import java.util.ArrayList
import scala.collection.JavaConversions._
import org.apache.commons.io.FileUtils
import akka.dispatch.Future
import Main.system

/** Downloads on multiple threads */
class Downloader(credentials: Credentials, bucketName: String, overwrite: Boolean) {
  private[aws] val s3 = new S3(credentials.accessKey, credentials.secretKey)

  def download(localDir: File): ArrayList[File] = {
    val results = new ArrayList[File]()
    val allNodes = s3.getAllObjectData(bucketName, "") // get every object
    allNodes foreach { node: S3ObjectSummary =>
      val outFile: File = new File(if (node.getKey.startsWith("/") || node.getKey.startsWith("\\"))
          node.getKey.substring(1) else node.getKey);
      results.add(outFile)
      try {
        if (node.getKey.endsWith("/")) {
          outFile.mkdirs
        } else {
          val fileName = node.getKey
          val overwriteExisting: Boolean = !(new File(fileName).exists()) || overwrite
          if (outFile.getParent!=null && overwriteExisting)
            outFile.getParentFile.mkdirs
          if (!fileName.endsWith("$folder$") && overwriteExisting)
            Future(downloadOne(outFile, node))(Main.system.dispatcher)
        }
      } catch {
        case ioe: IOException =>
          println(ioe)
      }
    }
    results
  }

  def downloadOne(outFile: File, node: S3ObjectSummary) {
    println("Downloading " + outFile + ", last modified " + node.getLastModified() + ", " + node.getSize + " bytes.")
    FileUtils.copyInputStreamToFile(s3.downloadFile(bucketName, node.getKey), outFile)
    outFile.setLastModified(node.getLastModified().getTime);
  }

  def deleteBadKeys {
    val allNodes = s3.getAllObjectData(bucketName, "") // get every object
    allNodes foreach { node: S3ObjectSummary =>
      val nodeKey = node.getKey()
      if (!nodeKey.startsWith("/"))
        s3.deleteObject(bucketName, nodeKey)
    }
  }
}
