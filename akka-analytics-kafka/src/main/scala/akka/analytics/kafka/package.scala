package akka.analytics

import akka.persistence.kafka.{DefaultEventDecoder, Event}

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka.KafkaUtils

import _root_.kafka.serializer.StringDecoder

package object kafka {
  implicit class JournalStreamingContext(context: StreamingContext) {
    def eventStream(kafkaParams: Map[String, String], kafkaTopics: Map[String, Int], storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER_2): DStream[Event] =
      KafkaUtils.createStream[String, Event, StringDecoder, DefaultEventDecoder](context, kafkaParams, kafkaTopics, storageLevel).map(_._2)
  }
}
