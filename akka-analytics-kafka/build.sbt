name := "akka-analytics-kafka"

fork in Test := true

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-persistence-experimental" % "2.3.4",
  "com.github.krasserm" %% "akka-persistence-kafka"        % "0.4",
  "com.github.krasserm" %% "akka-persistence-kafka"        % "0.4" % "test" classifier "tests",
  "org.apache.curator"   % "curator-test"                  % "2.7.1" % "test",
  "org.apache.spark"    %% "spark-streaming"               % "1.6.1",
  "org.apache.spark"    %% "spark-streaming-kafka"         % "1.6.1",
  "org.apache.kafka"    %% "kafka"                         % "0.8.2.1" excludeAll(
    ExclusionRule("javax.jms", "jms"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("com.sun.jmx", "jmxri")
  )
)
