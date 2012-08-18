package com.micronautics.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import java.io.File
import java.util.Date
import scala.collection.JavaConversions._
import Model._
import java.text.SimpleDateFormat


/**
 * @author Mike Slinn */
object Util {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' hh:mm:ss z")

  def dtFmt(time: Long): String = dateFormat.format(new Date(time)).trim

  def dtFmt(date: Date): String = dateFormat.format(date).trim

  /** @return -2 if s3File does not exist,
   *          -1 if s3File is older than local copy,
   *           0 if same age as local copy,
   *           1 if remote copy is newer,
   *           2 if local copy does not exist */
  def compareS3FileAge(file: File, node: S3ObjectSummary): Int = {
    if (!file.exists)
      return s3FileDoesNotExistLocally

    if (null==node)
      return s3FileDoesNotExist

    // Some OSes only truncate lastModified time to the nearest second, so truncate both times to nearest second
    val s3NodeLastModified: Long = node.getLastModified.getTime / 1000L
    val fileLastModified: Long = file.lastModified / 1000L
    //println("s3NodeLastModified=" + s3NodeLastModified + "; fileLastModified=" + fileLastModified)
    val result: Int = if (s3NodeLastModified == fileLastModified)
        s3FileSameAgeAsLocal
      else if (s3NodeLastModified < fileLastModified)
        s3FileIsOlderThanLocal
      else
        s3FileNewerThanLocal
      result
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
          return compareS3FileAge(file, node)
      } catch {
        case e: Exception =>
          System.out.println(e.getMessage() + ": " + key)
          return s3FileDoesNotExist
      }
    }
    s3FileDoesNotExist
  }
}
