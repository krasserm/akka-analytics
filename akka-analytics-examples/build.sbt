name := "akka-analytics-examples"

// -------------------------------------------------------------------------------------------------
//  Spark depends on Akka 2.2.3 but we need the serializers defined in akka-persistence 2.3.4.
//  Dependency definitions can be simplified once Spark upgrades to Akka 2.3.x
// -------------------------------------------------------------------------------------------------

libraryDependencies ++= Seq(
  "com.github.krasserm" %% "akka-persistence-kafka" % "0.2" intransitive(),
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.4" intransitive(),
  "org.apache.spark" %% "spark-core" % "1.0.1",
  "org.apache.spark" %% "spark-streaming-kafka" % "1.0.1"
)
