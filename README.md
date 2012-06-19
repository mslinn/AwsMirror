# Update AWS S3 content from local directory tree #

## Use Case ##
 1. Not using source control.
 2. Storing content on AWS S3 (CDN).
 3. AWS S3 serves a static web site from the content.

## Notes ##
AWS translates paths in the root directory; files in the root implicitly have keys that start with a leading slash.
This program stores each file name (referred to by AWS as a key) with a leading slash, and AWS S3 removes the leading
slash so you get a normal path name.
For example, assuming that the default file name is `index.html`,
`http://domain.com` and `http://domain.com/` are translated to `http://domain.com/index.html`.

For example, the key for a file in a directory called `/blah/ick/yuck.html` would be translated to `blah/ick/yuck.html`.

For each directory, AWS creates a file of the same name, with the suffix `_$folder$`.
If one of those files are deleted, the associated directory becomes unreachable.
These hidden files are ignored by this program; users never see them because they are for AWS S3 internal use only.

## To Run ##

````
git clone git@github.com:mslinn/awsMirror.git
cd awsMirror
sbt run
````

The help message shows all the subcommands:

````
Usage: aws <action>
  Where <action> is one of:
    auth                  provide authentication for an additional AWS account
                          delete - accountName delete authentication for specified AWS account name
                          list   - list authentications
                          modify - accountName modify authentication for specified AWS account name
    create [bucketName]   create specified bucket, or bucket specified in relevent .s3 file
    delete [bucketName]   delete specified bucket, or bucket specified in relevent .s3 file
    download              download directory tree from bucket specified in relevent .s3 file
    empty [bucketName]    empty specified bucket, or bucket specified in relevent .s3 file
    help                  print this message and exit
    link [accountName bucketName]
                          If accountName and bucketName are not specified, display contents of .s3 file in current directory or a parent directory.
                          Otherwise create or modify .s3 file in current directory by setting accountName and bucketName
    sync                  sync directory tree to specified bucket
    upload                upload directory tree to bucket specified in relevent .s3 file
                          -d delete files on AWS that are not in the local directory, after files are uploaded
````

You first nee to run the program with the `auth` option so it can create a file in your home directory called `.aws` to
hold your AWS access key and your AWS secret key.
You can store multiple authentications for each of the AWS accounts that you work with.

````
[{"awsAccountName":"memyselfi",
  "accessKey":"BLAHBLAH",
  "secretKey":"BLAHBLAHBLAHBLAHBLAHBLAH"}]
````

## Working with aws ##
The `.aws` file in your home directory contains authentication information, stored as JSON, for all AWS accounts
that you work with.
An `.s3` file defines the root of a source directory tree, and stores the account and bucket that the source directory
tree is associated with. It also stores the timestamp of the last sync. Its format is also JSON.
