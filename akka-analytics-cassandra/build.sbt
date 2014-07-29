name := "akka-analytics-cassandra"

// -------------------------------------------------------------------------------------------------
//  Spark depends on Akka 2.2.3 but we need the serializers defined in akka-persistence 2.3.4.
//  Dependency definitions can be simplified once Spark upgrades to Akka 2.3.x
// -------------------------------------------------------------------------------------------------

libraryDependencies ++= Seq(
  "com.datastax.spark" %% "spark-cassandra-connector" % "1.0.0-beta2",
  "com.google.protobuf" % "protobuf-java" % "2.5.0",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.4" % "provided",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "org.apache.spark" %% "spark-core" % "1.0.1" % "provided",
  "net.jpountz.lz4" % "lz4" % "1.2.0" % "runtime"
)

