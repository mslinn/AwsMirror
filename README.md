# Update AWS S3 content from local directory tree #

## To Run ##

    git clone git@github.com:mslinn/awsMirror.git
	cd awsMirror
	sbt run	
	
You first nee to run the program with the `login` option so it can create a file in your home directory called `.aws` to hold your AWS access key and your AWS secret key:

    { awsAccountName {
	    accessKey=34poslkflskeflsekjfl,
        secretKey=asdfoif3r3wfw3wgagawgawgawgw3taw3tatefef
      }
	}

## Use Case ##
 1. Not using source control.
 2. Storing content on AWS S3 (CDN).
 3. AWS S3 serves a static web site from the content.
