package akka.analytics

import scala.reflect.runtime.universe._

import akka.actor.ActorSystem
import akka.persistence.PersistentRepr
import akka.serialization.SerializationExtension

import com.datastax.spark.connector._
import com.datastax.spark.connector.types.TypeConverter

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

package object cassandra {
  implicit object JournalEntryTypeConverter extends TypeConverter[PersistentRepr] {
    val converter = implicitly[TypeConverter[Array[Byte]]]

    // FIXME: how to properly obtain an ActorSystem in Spark tasks?
    @transient lazy val system = ActorSystem("TypeConverter")
    @transient lazy val serial = SerializationExtension(system)

    def targetTypeTag = implicitly[TypeTag[PersistentRepr]]
    def convert(obj: Any): PersistentRepr = converter.convert(obj) match {
      case bytes if bytes.length == 1 => PersistentRepr(null, sequenceNr = -1L)
      case bytes                      => serial.deserialize(bytes, classOf[PersistentRepr]).get.update(sender = null)
    }
  }

  implicit class JournalSparkContext(context: SparkContext) {
    val keyspace = context.getConf.get("spark.cassandra.journal.keyspace", "akka")
    val table = context.getConf.get("spark.cassandra.journal.table", "messages")

    val journalKeyEventPair = (persistenceId: String, partition: Long, sequenceNr: Long, marker: String, message: PersistentRepr) =>
      (JournalKey(persistenceId, partition, sequenceNr, marker), message.payload)

    def eventTable(): RDD[(JournalKey, Any)] =
      context.cassandraTable(keyspace, table).as(journalKeyEventPair).filter(_._1.marker == "A")
  }
}
