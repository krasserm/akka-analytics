package akka.analytics.cassandra

import java.nio.ByteBuffer

import scala.util.{Failure, Success, Try}

import akka.actor.ActorSystem
import akka.analytics.cassandra.JournalContext.Ignore
import akka.persistence.PersistentRepr
import akka.serialization.{Serialization, SerializationExtension}
import com.datastax.driver.core.utils.Bytes
import com.datastax.spark.connector.CassandraRow
import com.typesafe.config.Config

// FIXME: Signal actual failures (e.g. wrong serializer)
private[cassandra] class EventSerializer(serializerConfig: Config) extends Serializable {

  // FIXME: how to shutdown actor system (?)
  @transient private[this] lazy val system = ActorSystem("TypeConverter", serializerConfig)
  @transient private[this] lazy val serial = SerializationExtension(system)

  private[this] def deserializeEvent(
      serialization: Serialization,
      row: CassandraRow
    ): Try[Any] =
    serialization.deserialize(
      row.getBytes("event").array,
      row.getInt("ser_id"),
      row.getString("ser_manifest"))

  private[this] def persistentFromByteBuffer(
      serialization: Serialization,
      b: ByteBuffer
    ): Try[PersistentRepr] =
    serialization.deserialize(Bytes.getArray(b), classOf[PersistentRepr])

  private[cassandra] def deserialize(row: CassandraRow): (JournalKey, Any) = {

    val m = row.getBytesOption("message") match {
      case None =>
        deserializeEvent(serial, row).map { p =>
          PersistentRepr(
            payload = p,
            sequenceNr = row.getLong("sequence_nr"),
            persistenceId = row.getString("persistence_id"),
            manifest = row.getString("event_manifest"),
            deleted = false,
            sender = null,
            writerUuid = row.getString("writer_uuid"))
        }

      case Some(b) =>
        persistentFromByteBuffer(serial, b) // for backwards compatibility
    }

    val filtered = m match {
      case Success(p) => p.update(sender = null)
      case Failure(f) => PersistentRepr(Ignore) // headers, confirmations, etc ...
    }

    (JournalKey(
      row.getString("persistence_id"),
      row.getLong("partition_nr"),
      row.getLong("sequence_nr")),
      filtered.payload)
  }
}
