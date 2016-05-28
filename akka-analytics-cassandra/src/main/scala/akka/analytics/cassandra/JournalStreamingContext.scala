package akka.analytics.cassandra

import akka.analytics.cassandra.JournalContext.{Ignore, IsJournalContext}
import com.datastax.spark.connector.ColumnRef
import com.datastax.spark.connector.streaming._
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext

class JournalStreamingContext(
    sc: StreamingContext,
    serializerConfig: Config = ConfigFactory.empty())
  extends IsJournalContext[RDD] {

  override def events(
      keyspace: String = "akka",
      table: String = "messages",
      serializer: EventSerializer,
      fields: Array[ColumnRef]
    ): RDD[(JournalKey, Any)] = {

    sc
      .cassandraTable(keyspace, table)
      .select(fields:_*)
      .map(serializer.deserialize)
      .filter(_._2 != Ignore)
  }
}
