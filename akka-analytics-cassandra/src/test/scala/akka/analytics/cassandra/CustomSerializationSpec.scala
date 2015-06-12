package akka.analytics.cassandra

import akka.actor._
import akka.persistence.PersistentActor
import akka.serialization.Serializer
import akka.testkit._

import com.typesafe.config.ConfigFactory

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.scalatest._

import scala.concurrent.duration._

object CustomSerializationSpec {
  val akkaConfig = ConfigFactory.parseString(
    """
      |akka.actor.serializers {
      |  example = "akka.analytics.cassandra.CustomSerializationSpec$ExamplePayloadSerializer"
      |}
      |akka.actor.serialization-bindings {
      |  "akka.analytics.cassandra.CustomSerializationSpec$ExamplePayload" = example
      |}
      |akka.persistence.journal.plugin = "cassandra-journal"
      |akka.persistence.snapshot-store.plugin = "cassandra-snapshot-store"
      |cassandra-journal.port = 9142
      |cassandra-snapshot-store.port = 9142
    """.stripMargin)

  val sparkConfig = new SparkConf()
    .setAppName("CassandraExample")
    .setMaster("local[4]")
    .set("spark.cassandra.connection.host", "127.0.0.1")
    .set("spark.cassandra.connection.native.port", "9142")
    .set("spark.cassandra.connection.rpc.port", "9171")


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
  val sparkContext = new SparkContext(sparkConfig)

  override protected def beforeAll(): Unit = {
    CassandraServer.start(60.seconds)
  }

  override protected def afterAll(): Unit = {
    sparkContext.stop()
    TestKit.shutdownActorSystem(system)
    CassandraServer.stop()
  }

  "akka-analytics-cassandra" must {
    "support custom serialization" in {
      val actor = system.actorOf(Props(new ExampleActor(testActor)))

      actor ! ExamplePayload("a")
      expectMsg(ExamplePayload("a"))

      val rdd: RDD[(JournalKey, Any)] = sparkContext.eventTable().cache()

      val actual = rdd.collect().head
      val expected = (JournalKey("test", 0, 1), ExamplePayload("a-ser-deser"))

      actual should be(expected)
    }
  }
}
