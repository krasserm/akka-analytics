package akka.analytics.cassandra

import scala.concurrent.duration._

import org.cassandraunit.utils.EmbeddedCassandraServerHelper

object CassandraServer {
  def start(timeout: FiniteDuration = 10.seconds) =
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(timeout.toMillis)

  def stop() =
    EmbeddedCassandraServerHelper.cleanEmbeddedCassandra()
}

