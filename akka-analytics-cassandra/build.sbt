name := "akka-analytics-cassandra"

fork in Test := true

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "com.datastax.spark"  %% "spark-cassandra-connector"  % "1.2.0-alpha1",
  "com.github.krasserm" %% "akka-persistence-cassandra" % "0.3.6"        % "test",
  "org.apache.cassandra" % "cassandra-all"              % "2.1.2"        % "test",
  "org.cassandraunit"    % "cassandra-unit"             % "2.0.2.2"      % "test" excludeAll(ExclusionRule("org.slf4j"))
)

