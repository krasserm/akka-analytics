name := "akka-analytics"

organization in ThisBuild := "com.github.krasserm"

version in ThisBuild := "0.3"

scalaVersion in ThisBuild := "2.11.6"

crossScalaVersions in ThisBuild := Seq("2.10.4", "2.11.6")

resolvers in ThisBuild += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

libraryDependencies in ThisBuild ++= Seq(
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit"                  % "2.3.11" % "test",
  "org.apache.spark"  %% "spark-core"                    % "1.4.0",
  "org.scalatest"     %% "scalatest"                     % "2.1.4" % "test"
)

lazy val root = (project.in(file("."))).aggregate(cassandra, kafka, examples)

lazy val cassandra = project.in(file("akka-analytics-cassandra"))

lazy val kafka = project.in(file("akka-analytics-kafka"))

lazy val examples = project.in(file("akka-analytics-examples")).dependsOn(cassandra, kafka)
