package akka.analytics

import scala.reflect.runtime.universe._
import scala.util._

import akka.actor.ActorSystem
import akka.persistence.PersistentRepr
import akka.serialization.SerializationExtension

import com.datastax.spark.connector._
import com.datastax.spark.connector.types.TypeConverter

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

package object cassandra {
  private case object Ignore

  implicit object JournalEntryTypeConverter extends TypeConverter[PersistentRepr] {
    val converter = implicitly[TypeConverter[Array[Byte]]]

    // FIXME: how to properly obtain an ActorSystem in Spark tasks?
    @transient lazy val system = ActorSystem("TypeConverter")
    @transient lazy val serial = SerializationExtension(system)

    def targetTypeTag = implicitly[TypeTag[PersistentRepr]]
    def convert(obj: Any): PersistentRepr = deserialize(converter.convert(obj))

    def deserialize(bytes: Array[Byte]): PersistentRepr = serial.deserialize(bytes, classOf[PersistentRepr]) match {
      case Success(p) => p.update(sender = null)
      case Failure(_) => PersistentRepr(Ignore) // headers, confirmations, etc ...
    }
  }

  implicit class JournalSparkContext(context: SparkContext) {
    val keyspace = context.getConf.get("spark.cassandra.journal.keyspace", "akka")
    val table = context.getConf.get("spark.cassandra.journal.table", "messages")

    val journalKeyEventPair = (persistenceId: String, partition: Long, sequenceNr: Long, message: PersistentRepr) =>
      (JournalKey(persistenceId, partition, sequenceNr), message.payload)

    def eventTable(): RDD[(JournalKey, Any)] =
      context.cassandraTable(keyspace, table).select("processor_id", "partition_nr", "sequence_nr", "message").as(journalKeyEventPair).filter(_._2 != Ignore)
  }
}
