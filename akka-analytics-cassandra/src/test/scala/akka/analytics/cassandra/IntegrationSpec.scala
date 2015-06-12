package akka.analytics.cassandra

import akka.persistence.PersistentActor
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

import akka.actor._
import akka.testkit._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.scalatest._

object IntegrationSpec {
  val akkaConfig = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "cassandra-journal"
      |akka.persistence.snapshot-store.plugin = "cassandra-snapshot-store"
      |cassandra-journal.port = 9142
      |cassandra-journal.max-partition-size = 3
      |cassandra-snapshot-store.port = 9142
    """.stripMargin)

  val sparkConfig = new SparkConf()
    .setAppName("CassandraExample")
    .setMaster("local[4]")
    .set("spark.cassandra.connection.host", "127.0.0.1")
    .set("spark.cassandra.connection.native.port", "9142")
    .set("spark.cassandra.connection.rpc.port", "9171")

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

import IntegrationSpec._

class IntegrationSpec extends TestKit(ActorSystem("test", akkaConfig)) with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  override protected def beforeAll(): Unit = {
    CassandraServer.start(60.seconds)
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    CassandraServer.stop()
  }

  "akka-analytics-cassandra" must {
    "expose journaled events as RDD" in {
      val actor = system.actorOf(Props(new ExampleActor(testActor)))
      val num = 10

      1 to num foreach { i => actor ! s"A-${i}" }
      1 to num foreach { i => expectMsg(s"A-${i}") }

      val sc = new SparkContext(sparkConfig)
      val rdd: RDD[(JournalKey, Any)] = sc.eventTable().cache()

      val actual = rdd.sortByKey().collect()
      val expected = 1 to num map { i =>
        (JournalKey("test", (i - 1) / 3, i), s"A-${i}")
      }

      actual should be(expected)
    }
  }
}
