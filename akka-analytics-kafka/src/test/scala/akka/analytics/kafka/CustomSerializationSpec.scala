package akka.analytics.kafka

import java.io.File

import akka.actor._
import akka.persistence.PersistentActor
import akka.persistence.kafka.Event
import akka.persistence.kafka.server._
import akka.serialization.Serializer
import akka.testkit._

import com.typesafe.config.ConfigFactory

import org.apache.commons.io.FileUtils
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.DStream
import org.scalatest._

object CustomSerializationSpec {
  val akkaConfig = ConfigFactory.parseString(
    """
      |akka.actor.serializers {
      |  example = "akka.analytics.kafka.CustomSerializationSpec$ExamplePayloadSerializer"
      |}
      |akka.actor.serialization-bindings {
      |  "akka.analytics.kafka.CustomSerializationSpec$ExamplePayload" = example
      |}
      |akka.persistence.journal.plugin = "kafka-journal"
      |akka.persistence.snapshot-store.plugin = "kafka-snapshot-store"
      |akka.test.single-expect-default = 10s
      |kafka-journal.event.producer.request.required.acks = 1
      |test-server.zookeeper.dir = target/test/zookeeper
      |test-server.kafka.log.dirs = target/test/kafka
    """.stripMargin)

  val sparkConfig = new SparkConf()
    .setAppName("events-consumer")
    .setMaster("local[4]")

  val kafkaTopics = Map("events" -> 2)
  val kafkaParams = Map[String, String](
    "group.id" -> "events-consumer",
    "auto.commit.enable" -> "false",
    "auto.offset.reset" -> "smallest",
    "zookeeper.connect" -> "localhost:2181",
    "zookeeper.connection.timeout.ms" -> "10000")

  case class ExamplePayload(value: String)

  class ExamplePayloadSerializer(system: ExtendedActorSystem) extends Serializer {
    val ExamplePayloadClass = classOf[ExamplePayload]

    override def identifier: Int = 44085
    override def includeManifest: Boolean = true

    override def toBinary(o: AnyRef): Array[Byte] = o match {
      case ExamplePayload(value) =>
        s"${value}-ser".getBytes("UTF-8")
    }

    override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = manifest.get match {
      case ExamplePayloadClass =>
        val value = new String(bytes, "UTF-8")
        ExamplePayload(s"${value}-deser")
    }
  }

  class ExampleActor(probe: ActorRef) extends PersistentActor {
    override val persistenceId: String = "test"

    override def receiveCommand: Receive = {
      case msg => persist(msg) {
        case evt => probe ! evt
      }
    }

    override def receiveRecover: Receive = {
      case _ =>
    }
  }
}

import CustomSerializationSpec._

class CustomSerializationSpec extends TestKit(ActorSystem("test", akkaConfig)) with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  var server: TestServer = _
  var jsc: JournalStreamingContext = _

  override protected def beforeAll(): Unit = {
    val systemConfig = system.settings.config
    val serverConfig = new TestServerConfig(systemConfig.getConfig("test-server"))
    server = new TestServer(serverConfig)
    jsc = new StreamingContext(sparkConfig, Seconds(1)).withSerializerConfig(system.settings.config)
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    server.stop()
    FileUtils.deleteDirectory(new File("target/test"))
  }

  "akka-analytics-kafka" must {
    "support custom serialization" in {
      val actor = system.actorOf(Props(new ExampleActor(testActor)))

      actor ! ExamplePayload("a")
      expectMsg(ExamplePayload("a"))

      val es: DStream[Event] = jsc.eventStream(kafkaParams, kafkaTopics)
      es.foreachRDD(_.collect().foreach(testActor ! _))

      jsc.context.start()
      expectMsg(Event("test", 1L, ExamplePayload("a-ser-deser")))
      jsc.context.stop(stopSparkContext = true, stopGracefully = false)
    }
  }
}
