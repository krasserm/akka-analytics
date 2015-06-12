name := "akka-analytics-examples"

libraryDependencies ++= Seq(
  "com.github.krasserm" %% "akka-persistence-kafka" % "0.3.4",
  "org.apache.spark"    %% "spark-streaming-kafka"  % "1.4.0"
)
