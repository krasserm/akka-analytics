package akka.analytics.cassandra

import akka.analytics.cassandra.JournalContext.IsJournalContext
import com.datastax.spark.connector.ColumnRef
import com.typesafe.config.{Config, ConfigFactory}

object JournalContext {
  trait IsJournalContext[R[_]] {
    def events(
        keyspace: String = "akka",
        table: String = "messages",
        serializer: EventSerializer,
        fields: Array[ColumnRef]
      ): R[(JournalKey, Any)]
  }

  private[cassandra] case object Ignore
}

private[cassandra] class JournalContext[SC, R[_]: IsJournalContext] (
    val sc: SC,
    serializerConfig: Config = ConfigFactory.empty()) {

  private var serializerOption: Option[EventSerializer] = None

  def withSerializerConfig(config: Config): JournalContext[SC, R] = {
    val journalContext = new JournalContext[SC, R](sc, config)
    journalContext.serializerOption = serializerOption
    journalContext
  }

  def eventTable(
      keyspace: String = "akka",
      table: String = "messages"
    ): R[(JournalKey, Any)] = {

    val serializer = serializerOption match {
      case Some(c) => c
      case None =>
        val newSerializer = new EventSerializer(serializerConfig)
        serializerOption = Some(newSerializer)
        newSerializer
    }

    implicitly[IsJournalContext[R]]
      .events(
        keyspace,
        table,
        serializer,
        Array(
          "persistence_id",
          "partition_nr",
          "sequence_nr",
          "event",
          "ser_id",
          "ser_manifest",
          "message",
          "event_manifest",
          "writer_uuid"))
  }
}
