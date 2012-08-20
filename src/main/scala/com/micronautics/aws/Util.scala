package com.micronautics.aws

import com.amazonaws.services.s3.model.S3ObjectSummary
import java.io.File
import java.util.Date
import scala.collection.JavaConversions._
import Model._
import java.text.SimpleDateFormat
import grizzled.math.stats._
import java.util.ArrayList
import scala.collection.JavaConversions._

/**
 * @author Mike Slinn */
object Util {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' hh:mm:ss z")

  def dtFmt(time: Long): String = dateFormat.format(new Date(time)).trim

  def dtFmt(date: Date): String = dateFormat.format(date).trim

  /** Type erasure means that Java interop does not allow the parameters to be specified as ArrayList[Long] */
  def computeStats(modificationTimes: ArrayList[_], deletionTimes: ArrayList[_]): String = {
    val editResult = computeStatString("Edit time", modificationTimes.asInstanceOf[ArrayList[Long]])
    val deleteResult = computeStatString("Deletion time", deletionTimes.asInstanceOf[ArrayList[Long]])
    if (editResult.length>0 && deleteResult.length>0)
      return editResult + "\n" + deleteResult

    if (editResult.length>0)
      return editResult

    if (deleteResult.length>0)
      return deleteResult

    return ""
  }

  def computeStatString(label: String, values: ArrayList[Long]): String = {
    if (values.length==0)
      return ""

    if (values.length==1)
      return "1 value: " + values(0) + " ms";

    val millisMean = arithmeticMean(values: _*).asInstanceOf[Long]

    if (values.length<5)
      return "%s mean of %d values: %d ms".format(label, values.length, millisMean)

    // std deviation is +/- so subtract from mean and double it to show uncertainty range
    // midpoint of uncertainty is therefore the mean
    val stdDev = popStdDev(values: _*).asInstanceOf[Long]
    val result = "%s mean of %d values: %d ms, +/- %d ms (1 std dev: from %d ms to %d ms, 2 std devs: from %d ms to %d ms)".
      format(label, values.length, millisMean, stdDev, millisMean - stdDev, millisMean + stdDev, millisMean - 2*stdDev, millisMean + 2*stdDev)
    return result
  }

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
    val fileLastModified: Long = (file.lastModified + 500L) / 1000L // round to nearest second
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
