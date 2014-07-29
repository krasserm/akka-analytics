name := "akka-analytics"

organization in ThisBuild := "com.github.krasserm"

version in ThisBuild := "0.2-SNAPSHOT"

scalaVersion in ThisBuild := "2.10.4"

lazy val root = (project.in(file("."))).aggregate(cassandra, kafka, examples)

lazy val cassandra = project.in(file("akka-analytics-cassandra"))

lazy val kafka = project.in(file("akka-analytics-kafka"))

lazy val examples = project.in(file("akka-analytics-examples")).dependsOn(cassandra, kafka)
