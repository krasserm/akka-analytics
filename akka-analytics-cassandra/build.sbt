name := "akka-analytics-cassandra"

fork in Test := true

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "com.datastax.spark"  %% "spark-cassandra-connector"  % "1.3.0-M1",
  "com.github.krasserm" %% "akka-persistence-cassandra" % "0.3.8"        % "test",
  "org.apache.cassandra" % "cassandra-all"              % "2.1.5"        % "test",
  "org.cassandraunit"    % "cassandra-unit"             % "2.0.2.2"      % "test" excludeAll(ExclusionRule("org.slf4j"))
)

