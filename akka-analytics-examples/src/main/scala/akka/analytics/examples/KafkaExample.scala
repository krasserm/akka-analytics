package akka.analytics.examples

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.DStream

import akka.analytics.kafka._
import akka.persistence.kafka.Event

object KafkaExample extends App {
  val sparkConf = new SparkConf()
    .setAppName("events-consumer")
    .setMaster("local[4]")

  val topics = Map("events" -> 2)
  val params = Map[String, String](
    "group.id" -> "events-consumer",
    "auto.commit.enable" -> "false",
    "auto.offset.reset" -> "smallest",
    "zookeeper.connect" -> "localhost:2181",
    "zookeeper.connection.timeout.ms" -> "10000")

  val ssc = new StreamingContext(sparkConf, Seconds(1))
  val es: DStream[Event] = ssc.eventStream(params, topics)

  es.foreachRDD(_.collect().foreach(println))

  ssc.start()
  ssc.awaitTermination()
}
