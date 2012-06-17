// see https://github.com/sbt/sbt-assembly
//import AssemblyKeys._ // put this at the top of the file

organization := "Micronautics Research"

name := "awsMirror"

version := "0.1"

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-deprecation")

scalacOptions in (Compile, doc) <++= baseDirectory.map {
  (bd: File) => Seq[String](
     "-sourcepath", bd.getAbsolutePath,
     "-doc-source-url", "https://bitbucket.org/mslinn/akkafutures/src/9fa9548ce587/akkaFutures�{FILE_PATH}.scala"
  )
}

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"
)

libraryDependencies ++= Seq(
  "com.amazonaws"                 %  "aws-java-sdk"        % "1.3.10" withSources(),
  "commons-io"                    %  "commons-io"          % "2.3"    withSources(),
  "com.codahale"                  %  "jerkson_2.9.1"       % "0.5.0",
  "junit"                         %  "junit"               % "4.10"   % "test" withSources(),
  "org.scribe"                    %  "scribe"              % "1.3.1"  withSources(),
  "com.github.scala-incubator.io" %  "scala-io-core_2.9.1" % "0.4.0"  withSources(),
  "com.github.scala-incubator.io" %  "scala-io-file_2.9.1" % "0.4.0"  withSources()
)

//seq(assemblySettings: _*)

logLevel := Level.Error

//{System.setProperty("jline.terminal", "none"); seq()} // Windows only

// define the statements initially evaluated when entering 'console', 'console-quick', or 'console-project'
initialCommands := """
  import java.net.URL
  import java.util.concurrent.Executors
  import java.util.Date
  import scala.MatchError
  import scala.util.Random
  import scalax.io.JavaConverters.asInputConverter
  import scalax.io.Codec
  """

// Only show warnings and errors on the screen for compilations.
// This applies to both test:compile and compile and is Info by default
logLevel in compile := Level.Warn
