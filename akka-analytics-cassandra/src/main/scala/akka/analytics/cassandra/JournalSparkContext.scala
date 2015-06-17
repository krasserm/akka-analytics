package akka.analytics.cassandra

import akka.actor.ActorSystem
import akka.persistence.PersistentRepr
import akka.serialization.SerializationExtension

import com.datastax.spark.connector._
import com.datastax.spark.connector.types._
import com.typesafe.config.{ConfigFactory, Config}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

private case object Ignore

class JournalEntryTypeConverter(config: Config) extends TypeConverter[PersistentRepr] {
  import scala.reflect.runtime.universe._
  import scala.util._

  val converter = implicitly[TypeConverter[Array[Byte]]]

  // FIXME: how to shutdown actor system (?)
  @transient lazy val system = ActorSystem("TypeConverter", config)
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

class JournalSparkContext(val context: SparkContext, serializerConfig: Config = ConfigFactory.empty()) {
  private var converterOption: Option[JournalEntryTypeConverter] = None

  def withSerializerConfig(config: Config): JournalSparkContext = {
    val ctx = new JournalSparkContext(context, config)
    ctx.converterOption = converterOption
    ctx
  }

  def eventTable(keyspace: String = "akka", table: String = "messages"): RDD[(JournalKey, Any)] = {
    implicit val converter = converterOption match {
      case Some(c) => c
      case None =>
        val newConverter = new JournalEntryTypeConverter(serializerConfig)
        converterOption = Some(newConverter)
        TypeConverter.registerConverter(newConverter)
        newConverter
    }
    context.cassandraTable(keyspace, table).select("processor_id", "partition_nr", "sequence_nr", "message").as(JournalSparkContext.journalKeyEventPair).filter(_._2 != Ignore)
  }
}

private object JournalSparkContext {
  val journalKeyEventPair = (persistenceId: String, partition: Long, sequenceNr: Long, message: PersistentRepr) =>
    (JournalKey(persistenceId, partition, sequenceNr), message.payload)
}