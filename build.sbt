
import com.typesafe.startscript.StartScriptPlugin

organization := "Micronautics Research"

name := "awsMirror"

version := "0.1"

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-deprecation")

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

scalacOptions in (Compile, doc) <++= baseDirectory.map {
  (bd: File) => Seq[String](
     "-sourcepath", bd.getAbsolutePath,
     "-doc-source-url", "https://github.com/mslinn/AwsMirror/tree/master€{FILE_PATH}.scala"
  )
}

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"
)

libraryDependencies ++= Seq(
  "ch.qos.logback"                %  "logback-classic"     % "1.0.0"  withSources(),
  "com.amazonaws"                 %  "aws-java-sdk"        % "1.3.13" withSources(),
  "commons-io"                    %  "commons-io"          % "2.4"    withSources(),
  "com.codahale"                  %  "jerkson_2.9.1"       % "0.5.0",
  "junit"                         %  "junit"               % "4.10"   % "test" withSources(),
  "com.github.scala-incubator.io" %  "scala-io-core_2.9.1" % "0.4.0"  withSources(),
  "com.github.scala-incubator.io" %  "scala-io-file_2.9.1" % "0.4.0"  withSources(),
  "org.scala-tools.time"          %  "time_2.9.1"          % "0.5",
  "org.scribe"                    %  "scribe"              % "1.3.1"  withSources(),
  "org.slf4j"                     %  "slf4j-api"           % "1.6.5"  withSources()
)

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

logLevel := Level.Error

//{System.setProperty("jline.terminal", "none"); seq()} // Windows only

// define the statements initially evaluated when entering 'console', 'console-quick', or 'console-project'
initialCommands := """
  import java.net.URL
  import java.util.Date
  import scalax.io.JavaConverters.asInputConverter
  import scalax.io.Codec
  """

// Only show warnings and errors on the screen for compilations.
// This applies to both test:compile and compile and is Info by default
logLevel in compile := Level.Warn
