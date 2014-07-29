name := "akka-analytics-kafka"

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

// -------------------------------------------------------------------------------------------------
//  Spark depends on Akka 2.2.3 but we need the serializers defined in akka-persistence 2.3.4.
//  Dependency definitions can be simplified once Spark upgrades to Akka 2.3.x
// -------------------------------------------------------------------------------------------------

libraryDependencies ++= Seq(
  "com.google.protobuf" % "protobuf-java" % "2.5.0",
  "com.github.krasserm" %% "akka-persistence-kafka" % "0.2" % "provided",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.4" % "provided",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "org.apache.spark" %% "spark-streaming-kafka" % "1.0.1"  % "provided",
  "org.apache.kafka" %% "kafka" % "0.8.1.1" excludeAll(
    ExclusionRule("javax.jms", "jms"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("com.sun.jmx", "jmxri")
  )
)
