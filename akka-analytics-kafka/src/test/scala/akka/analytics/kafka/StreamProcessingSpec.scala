package akka.analytics.kafka

import java.io.File

import akka.actor._
import akka.persistence.PersistentActor
import akka.persistence.kafka.Event
import akka.persistence.kafka.server._
import akka.testkit._

import com.typesafe.config.ConfigFactory

import org.apache.commons.io.FileUtils
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.DStream
import org.scalatest._

object StreamProcessingSpec {
  val akkaConfig = ConfigFactory.parseString(
    """
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

  class ExampleActor(probe: ActorRef) extends PersistentActor {
    override val persistenceId: String = "test"

    override def receiveCommand: Receive = {
      case s: String => persist(s) {
        case s: String => probe ! s
      }
    }

    override def receiveRecover: Receive = {
      case _ =>
    }
  }
}

import StreamProcessingSpec._

class StreamProcessingSpec extends TestKit(ActorSystem("test", akkaConfig)) with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  var server: TestServer = _
  var ssc: StreamingContext = _

  override protected def beforeAll(): Unit = {
    val systemConfig = system.settings.config
    val serverConfig = new TestServerConfig(systemConfig.getConfig("test-server"))
    server = new TestServer(serverConfig)
    ssc = new StreamingContext(sparkConfig, Seconds(1))
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    server.stop()
    FileUtils.deleteDirectory(new File("target/test"))
  }

  "akka-analytics-kafka" must {
    "expose journaled events as DStream" in {
      val actor = system.actorOf(Props(new ExampleActor(testActor)))
      val num = 10

      1 to num foreach { i => actor ! s"A-${i}" }
      1 to num foreach { i => expectMsg(s"A-${i}") }

      val es: DStream[Event] = ssc.eventStream(kafkaParams, kafkaTopics)
      es.foreachRDD(_.collect().foreach(testActor ! _))

      ssc.start()
      1 to num foreach { i => expectMsg(Event("test", i, s"A-${i}")) }
      ssc.stop(stopSparkContext = true, stopGracefully = false)
    }
  }
}
