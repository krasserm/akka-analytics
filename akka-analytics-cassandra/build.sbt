name := "akka-analytics-cassandra"

fork in Test := true

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "com.typesafe.akka"    %% "akka-persistence"           % "2.4.6",
  "org.apache.spark"     %% "spark-sql"                  % "1.6.1",
  "com.datastax.spark"   %% "spark-cassandra-connector"  % "1.5.0",
  "org.apache.cassandra" %  "cassandra-clientutil"       % "3.5",
  "org.apache.spark"     %% "spark-streaming"            % "1.6.1",
  "org.apache.commons"   %  "commons-lang3"              % "3.1",
  "com.typesafe.akka"    %% "akka-persistence-cassandra" % "0.14"    % "test",
  "org.apache.cassandra" %  "cassandra-all"              % "3.5"     % "test",
  "org.cassandraunit"    %  "cassandra-unit"             % "3.0.0.1" % "test" excludeAll ExclusionRule("org.slf4j")
)

