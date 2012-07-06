package com.micronautics.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import java.io.{IOException, File}
import java.util.{Date, ArrayList}
import scala.collection.JavaConversions._
import org.apache.commons.io.FileUtils
import akka.dispatch.{ExecutionContext, Await, Future}
import akka.util.Duration
import collection.mutable

/** Downloads on multiple threads */
class Downloader(credentials: Credentials, bucketName: String, overwrite: Boolean) {
  private[aws] val s3 = new S3(credentials.accessKey, credentials.secretKey)
  private val futures = mutable.ListBuffer.empty[Future[Boolean]]
  private implicit val dispatcher: ExecutionContext = Main.system.dispatcher

  def download(localDir: File): ArrayList[File] = {
    val results = new ArrayList[File]()
    val allNodes = s3.getAllObjectData(bucketName, "") // get every object
    allNodes foreach { node: S3ObjectSummary =>
      val outFile: File = new File(if (node.getKey.startsWith("/") || node.getKey.startsWith("\\"))
          node.getKey.substring(1) else node.getKey)
      results.add(outFile)
      try {
        if (node.getKey.endsWith("/")) {
          outFile.mkdirs
        } else {
          val fileName = node.getKey
          val file = new File(localDir, fileName)
          val overwriteExisting: Boolean = !(file.exists()) || overwrite
          if (outFile.getParent!=null && overwriteExisting)
            outFile.getParentFile.mkdirs
          val s3FileNewer = s3FileIsNewer(file, node)
          if (!fileName.endsWith("$folder$") && overwriteExisting  && s3FileNewer) // todo implement .s3ignore
            futures += Future(downloadOne(outFile, node))
        }
      } catch {
        case ioe: IOException =>
          println(ioe)
      }
    }
    //Await.ready(Future.sequence(futures.toSeq), Duration.Inf) // block until all downloads are complete
    results
  }

  def s3FileIsNewer(file: File, node: S3ObjectSummary): Boolean = {
    if (!file.exists) return true
    val s3NodeLastModified: Date = node.getLastModified
    val isNewer: Boolean = s3NodeLastModified.getTime > file.lastModified
//    System.out.println("s3NodeLastModified.getTime()=" + s3NodeLastModified.getTime +
//      "; file.lastModified()=" + file.lastModified + "; newer=" + isNewer)
    return isNewer
  }

  def downloadOne(outFile: File, node: S3ObjectSummary) = {
    println("Downloading " + outFile + ", last modified " + node.getLastModified() + ", " + node.getSize + " bytes.")
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
