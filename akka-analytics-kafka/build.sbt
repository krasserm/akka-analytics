name := "akka-analytics-kafka"

fork in Test := true

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "com.github.krasserm" %% "akka-persistence-kafka" % "0.3.4",
  "org.apache.spark"    %% "spark-streaming"        % "1.2.0",
  "org.apache.spark"    %% "spark-streaming-kafka"  % "1.2.0",
  "org.apache.kafka"    %% "kafka"                  % "0.8.2-beta" excludeAll(
    ExclusionRule("javax.jms", "jms"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("com.sun.jmx", "jmxri")
  )
)
