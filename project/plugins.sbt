//libraryDependencies <+= sbtVersion { v =>
  // No SBT 0.11.3 support yet
  //"com.github.siasia" %% "xsbt-proguard-plugin" % (v + "-0.1.1")
//  "com.github.siasia" %% "xsbt-proguard-plugin" % ("0.11.2-0.1.1")
//}

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.3.0-SNAPSHOT")
