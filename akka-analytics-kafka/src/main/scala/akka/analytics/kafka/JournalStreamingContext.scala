package akka.analytics.kafka

import _root_.kafka.serializer.{Decoder, StringDecoder}

import akka.persistence.kafka.Event

import com.typesafe.config._

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka.KafkaUtils

import scala.reflect.ClassTag

class JournalStreamingContext(val context: StreamingContext, serializerConfig: Config = ConfigFactory.empty()) {
  def withSerializerConfig(config: Config): JournalStreamingContext =
    new JournalStreamingContext(context, config)

  def eventStream(kafkaParams: Map[String, String], kafkaTopics: Map[String, Int], storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER_2): DStream[Event] =
    eventStreamWithDecoder[JournalEventDecoder](kafkaParams, kafkaTopics, storageLevel)

  def eventStreamWithDecoder[A <: Decoder[Event] : ClassTag](kafkaParams: Map[String, String], kafkaTopics: Map[String, Int], storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER_2): DStream[Event] =
    KafkaUtils.createStream[String, Event, StringDecoder, A](context, kafkaParams + ("akka.analytics.serializer.config" -> serializerConfig.root().render()), kafkaTopics, storageLevel).map(_._2)
}
