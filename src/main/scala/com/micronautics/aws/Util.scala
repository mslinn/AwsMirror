package com.micronautics.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import java.io.File
import java.util.Date
import scala.collection.JavaConversions._
import Model._


/**
 * @author Mike Slinn */
object Util {

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

      val s3NodeLastModified: Date = node.getLastModified
      val result: Int = if (s3NodeLastModified.getTime == file.lastModified)
          s3FileSameAgeAsLocal
        else if (s3NodeLastModified.getTime < file.lastModified)
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
