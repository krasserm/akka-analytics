package akka.analytics

import akka.actor.ActorSystem
import akka.persistence.PersistentRepr
import akka.serialization.SerializationExtension

import com.datastax.spark.connector._
import com.datastax.spark.connector.types._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

package object cassandra {
  private case object Ignore

  implicit object JournalEntryTypeConverter extends TypeConverter[PersistentRepr] {
    import scala.reflect.runtime.universe._
    import scala.util._

    val converter = implicitly[TypeConverter[Array[Byte]]]

    // FIXME: how to properly obtain an ActorSystem in Spark tasks?
    @transient lazy val system = ActorSystem("TypeConverter")
    @transient lazy val serial = SerializationExtension(system)

    def targetTypeTag = implicitly[TypeTag[PersistentRepr]]
    def convertPF = {
      case obj => deserialize(converter.convert(obj))
    }

    def deserialize(bytes: Array[Byte]): PersistentRepr = serial.deserialize(bytes, classOf[PersistentRepr]) match {
      case Success(p) => p.update(sender = null)
      case Failure(_) => PersistentRepr(Ignore) // headers, confirmations, etc ...
    }
  }

  TypeConverter.registerConverter(JournalEntryTypeConverter)

  implicit class JournalSparkContext(context: SparkContext) {
    val journalKeyEventPair = (persistenceId: String, partition: Long, sequenceNr: Long, message: PersistentRepr) =>
      (JournalKey(persistenceId, partition, sequenceNr), message.payload)

    def eventTable(keyspace: String = "akka", table: String = "messages"): RDD[(JournalKey, Any)] =
      context.cassandraTable(keyspace, table).select("processor_id", "partition_nr", "sequence_nr", "message").as(journalKeyEventPair).filter(_._2 != Ignore)
  }
}
