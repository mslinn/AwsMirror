<%@page import="com.amazonaws.services.s3.AmazonS3Client, com.amazonaws.services.s3.model.Bucket, com.amazonaws.services.s3.model.BucketWebsiteConfiguration, com.amazonaws.services.s3.model.GetBucketWebsiteConfigurationRequest, com.amazonaws.services.s3.model.ListBucketsRequest, com.micronautics.aws.BBContext, com.micronautics.aws.S3" %>
<html>
<body>
<h2>Your AWS S3 Buckets</h2><%
S3 s3 = new S3();
if (s3.exception!=null) {
  %>Exception: <tt><%=s3.exception%></tt><br/><%
} else {
  %><ul><%
  String action = request.getParameter("action");
  if ("Repopulate"==action) {
      String bucketName = request.getParameter("bucketName");
      BBContext bbContext = (BBContext)pageContext.getAttribute("bbContext");
      s3.repopulate(bucketName, bbContext);
  }
  AmazonS3Client amazonS3Client = new AmazonS3Client(s3.awsCredentials);
  ListBucketsRequest listBucketsRequest = new ListBucketsRequest();
  for (Bucket bucket : amazonS3Client.listBuckets(listBucketsRequest)) {
      GetBucketWebsiteConfigurationRequest gbwcr = new GetBucketWebsiteConfigurationRequest(bucket.getName());
      BucketWebsiteConfiguration bwc = amazonS3Client.getBucketWebsiteConfiguration(gbwcr);
      // endpoint is not returned in BucketWebsiteConfiguration!
      String endpoint = "s3-website-us-east-1.amazonaws.com";
     %><li><tt><a href="http://<%=bucket.getName() + "." + endpoint%>" target=_blank><%=bucket.getName()%></a></tt>
    <form action="index.jsp" method="post" style="display: inline;">
        <input type="submit" name="action" value="Repopulate"/>
        <input type="hidden" name="bucketName" value="<%=bucket.getName()%>"/>
    </form>
</li><%
  }
} %></ul>
<hr />
<a href="showBitBucketPost.jsp">showBitBucketPost</a>
</body>
</html>
