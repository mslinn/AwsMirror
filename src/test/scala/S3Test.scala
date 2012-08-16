import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.micronautics.aws.Main
import com.micronautics.aws.S3
import com.micronautics.aws.S3File
import java.io.File
import java.util.Date
import com.micronautics.aws.S3.relativize
import org.apache.commons.io.FileUtils
import org.apache.http.{ HttpEntity, HttpResponse }
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, WordSpec}
import org.scalatest.matchers.MustMatchers

/**These tests will fail unless a file called AwsCredentials.properties is created in src/test/resources. */
class S3Test extends WordSpec with MustMatchers with BeforeAndAfter with BeforeAndAfterAll {
  val bucketName = "test" + new Date().getTime
  val file1Name = "index.html"
  val file2Name = "index2.html"
  val file1 = new File(file1Name)
  val file2 = new File(file2Name)
  val s3File1: S3File = Main.readS3File
  val creds = Main.getAuthentication(s3File1.accountName)

  val s3: S3 = creds match {
    case Some(credentials) =>
      new S3(credentials.accessKey, credentials.secretKey)

    case None =>
      fail("Cannot locate .aws file")
  }
  val s3File = s3File1.copy(bucketName=this.bucketName)

  override def afterAll() {
    s3.deleteBucket(bucketName)
    if (file2.exists)
      file2.delete
  }

  override def beforeAll() {
//    println("Creating bucket " + bucketName)
    s3.createBucket(bucketName)
  }

  "S3 operations" must {
    "ensure file to upload can be found" in {
      assert(file1.exists, "Ensure file to upload can be found")
    }

    "upload" in {
      s3.uploadFile(bucketName, file1Name, file1)
      val item: S3ObjectSummary = s3.getOneObjectData(bucketName, file1Name)
      assert(null != item, "Upload succeeded")
      assert(item.getKey.compareTo(relativize(file1Name)) == 0, "Upload key matches filename")

      assert("https://" + bucketName + ".s3.amazonaws.com/" === s3.getResourceUrl(bucketName, ""), "Correct access URL")
    }

    "pretty-print JSON" in {
      val contents = """{"accountName":"memyselfi","bucketName":"blah","ignores":[".*~",".*.aws",".*.git",".*.s3",".*.svn",".*.tmp","cvs"],"endpoint":"s3-website-us-east-1.amazonaws.com"}"""
      val result = contents.replaceAll("(.*?:(\\[.*?\\],|.*?,))", "$0\n ")
      println(result)
      assert(
        """{"accountName":"memyselfi",
          | "bucketName":"blah",
          | "ignores":[".*~",".*.aws",".*.git",".*.s3",".*.svn",".*.swp",".*.tmp","cvs"],
          | "endpoint":"s3-website-us-east-1.amazonaws.com"}""".stripMargin === result, "PrettyPrinted JSON")
    }

    "download" in {
      s3.uploadFile(bucketName, file1Name, file1)

      assert(null != s3File, ".s3 file not found")

//      println(s3File.endpointUrl)
      val contents = httpGet(s3File.endpointUrl + "/index.html")
      assert(contents != null, s3File.endpointUrl + " is invalid")
      assert(FileUtils.readFileToString(file1) === contents, "Wrong contents")

      FileUtils.copyInputStreamToFile(s3.downloadFile(bucketName, file1Name), file2)
      assert(file2.exists, "Ensure downloaded file can be found")
      assert(file2.length === file1.length, "Ensure downloaded file is complete")

      FileUtils.copyInputStreamToFile(s3.downloadFile(bucketName, "/" + file1Name), file2)
      assert(file2.exists, "Ensure downloaded file can be found")
      assert(file2.length === file1.length, "Ensure downloaded file is complete")
    }
  }

  val httpclient: HttpClient = new DefaultHttpClient

  def httpGet(url: String): String = {
//    println("Getting from url=" + url)
    val httpGet: HttpGet = new HttpGet(url)
    val response: HttpResponse = httpclient.execute(httpGet)
    val entity: HttpEntity = response.getEntity
    if (entity==null) null else EntityUtils.toString(entity)
  }
}
