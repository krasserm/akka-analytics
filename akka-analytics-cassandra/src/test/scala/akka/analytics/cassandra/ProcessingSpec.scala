package akka.analytics.cassandra

import scala.concurrent.duration._

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.analytics.cassandra.ProcessingSpec._
import akka.persistence.PersistentActor
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.apache.spark.SparkConf
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

object ProcessingSpec {
  val akkaConfig = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "cassandra-journal"
      |akka.persistence.snapshot-store.plugin = "cassandra-snapshot-store"
      |cassandra-journal.port = 9142
      |cassandra-journal.target-partition-size = 3
      |cassandra-snapshot-store.port = 9142
    """.stripMargin)

  val sparkConfig = new SparkConf()
    .setAppName("CassandraExample")
    .setMaster("local[4]")
    .set("spark.cassandra.connection.host", "127.0.0.1")
    .set("spark.cassandra.connection.port", "9142")

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

abstract class ProcessingSpec[SC]
  extends TestKit(ActorSystem("test", akkaConfig))
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  def underTest: SC

  def execute(sc: SC): Array[(JournalKey, Any)]

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
      1 to num foreach { i => expectMsg(20.seconds, s"A-${i}") }

      val actual = execute(underTest)
      val expected = 1 to num map { i =>
        (JournalKey("test", (i - 1) / 3, i), s"A-${i}")
      }

      actual should be(expected)
    }
  }
}
