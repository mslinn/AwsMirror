# Update AWS S3 content from local directory tree #

This project was sponsored by [Micronautics Research Corporation](http://www.micronauticsresearch.com/)

## Use Case ##
 1. Not using source control.
 2. Storing content on AWS S3 (CDN).
 3. AWS S3 serves a static web site from the content.

## To Build ##

### Start Script ###

The compiled program needs to be run from a script. `xsbt-start-script-plugin` creates that script.
See [my pull request](https://github.com/typesafehub/xsbt-start-script-plugin/issues/17) that improves the generated script.
If you install my modified version of `xsbt-start-script-plugin` then you should be able to run the script from any directory.
To do that:

````
mkdir ~/work
cd ~/work
git clone git://github.com/mslinn/xsbt-start-script-plugin.git
cd xsbt-start-script-plugin
sbt compile publish-local
````

### Building AwsMirror ###

 1. This program requires Java 7 or later.
 1. Point `JAVA_HOME` to a Java 7 JDK.
 1. Type the following into a bash console:
````
git clone git@github.com:mslinn/AwsMirror.git
cd AwsMirror
sbt compile start-script
````

 1. Add  `AwsMirror/target` to the `PATH` or write a script like this bash script to launch the program.
    The remainder of these instructions assume that a similar script exists somewhere on the path called `aws`:
````
#!/bin/bash
# Ensure that Java 7 is on the classpath; best if JAVA_HOME is also set
# Must set SBT_HOME to directory where sbt-launch.jar resides
export SBT_HOME=/opt
~/work/AwsMirror/target/start $*
````

## To Run ##

````
aws subcommandsGoHere
````

The help message shows all the subcommands:

````
Usage: aws <option> <action>
  Where <option> is one of:
      -m    multithreading enabled
      -M    multithreading disabled
      -v    less verbose output
      -V    more verbose output
  and <action> is one of:
    auth   provide authentication for an additional AWS account
                          add accountName      you will be prompted to add credentials for AWS accountName
                          delete accountName   delete authentication for specified AWS account name
                          list                 list authentications
                          modify accountName   modify authentication for specified AWS account name
    create [accountName bucketName]
        create specified bucket for accountName, or bucket specified in relevent .s3 file, enables web access and uploads a short index.html file
    delete [accountName bucketName]
        delete specified bucket from AWS account, or bucket specified in relevent .s3 file
    download, down
      download bucket specified in relevent .s3 file to the entire tree
    empty [bucketName]
      empty specified bucket, or bucket specified in relevent .s3 file
    help    print this message and exit
    link [accountName bucketName]
      If accountName and bucketName are not specified, display contents of .s3 file in current directory or a parent directory.
      Otherwise create or modify .s3 file in current directory by setting accountName and bucketName
    sync    sync directory tree to specified bucket
    upload, up  upload entire directory tree to bucket specified in relevent .s3 file
````

The `upload` and `sync` commands continue uploading changed files until you press Control-C or Command-C
(works on Linux, Windows and Mac).

### Run Sequence ###

 1. You first need to run the program with the `auth` option so it can create a file in your home directory called `.aws` to
hold your AWS access key and your AWS secret key for the AWS account you specify.
You can store multiple authentications for each of the AWS accounts that you work with.
This is a hidden file under Windows.
````
aws auth yourAccountName
````
The contents of the `.aws` file are in JSON format, and look something like this:
````
[{"awsAccountName":"memyselfi",
  "accessKey":"BLAHBLAH",
  "secretKey":"BLAHBLAHBLAHBLAHBLAHBLAH"}]
````
You can add more AWS accounts by running the same command again.

 2. Designate a directory to be the root of a directory tree that you want mirrored to AWS S3.
    Both of the following steps creates or modifies a file called `.s3` in the current directory.
    The `.s3` file defines the root of a source directory tree, and stores the account and bucket that the source directory
    tree is associated with. It also stores the timestamp of the last sync. Its format is JSON.
    This is a hidden file under Windows.
    The file looks something like this:
````
{"accountName":"memyselfi",
 "bucketName":"test789",
 "ignores":[".*~", ".*.aws", ".*.git", ".*.s3", ".*.svn", ".*.swp", ".*.tmp", "cvs"],
 "endpointUrl":"http://test789.s3.amazonaws.com/"}
````
    NOTE: The current version of awsMirror does not provide a user-friendly means of editing the ignored file patterns
    (which are regular expressions), nor the endpointUrl.

    From the directory you wish to be the mirror root, run one of the following commands.

  a) If the AWS S3 bucket you wish to mirror the directory tree to does not already exist:
````
aws create yourAccountName bucketName
Created bucket bucketName for AWS account yourAccountName
You can access the new bucket at https://bucketName.s3.amazonaws.com/
````

  b) If the AWS S3 bucket already exists:
````
aws link yourAccountName bucketName
````

 3. Sync the directory with the AWS S3 bucket:
````
aws sync
````

## Notes ##
When web site access is enabled, AWS content is accessed by paths constructed by concatentating the URL, a slash (/),
and the keyed data.
The keys must therefore consist of relative paths (relative directory name followed by a file name),
and must not start with a leading slash.
This program stores each file name (referred to by AWS as a key) without a leading slash.
For example, assuming that the default file name is `index.html`,
`http://domain.com` and `http://domain.com/` are translated to `http://domain.com/index.html`.

As another example, AwsMirror defines the key for a file in a directory called `{WEBROOT}/blah/ick/yuck.html` to `blah/ick/yuck.html`.

For each directory, AWS creates a file of the same name, with the suffix `_$folder$`.
If one of those files are deleted, the associated directory becomes unreachable. Don't mess with them.
These hidden files are ignored by this program; users never see them because they are for AWS S3 internal use only.
