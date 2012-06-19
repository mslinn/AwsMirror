package com.micronautics.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import java.io.{IOException, File}
import java.util.ArrayList
import scala.collection.JavaConversions._
import org.apache.commons.io.FileUtils

class Downloader(credentials: Credentials, bucketName: String) {
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
          if (outFile.getParent!=null)
            outFile.getParentFile.mkdirs
          if (!node.getKey.endsWith("$folder$")) {
            println("Downloading " + outFile + ", last modified " + node.getLastModified() + ", " + node.getSize + " bytes.")
            FileUtils.copyInputStreamToFile(s3.downloadFile(bucketName, node.getKey), outFile)
          }
        }
      } catch {
        case ioe: IOException =>
          println(ioe)
      }
    }
    results
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
