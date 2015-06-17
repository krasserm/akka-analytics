package akka.analytics.kafka

import akka.actor.ActorSystem
import akka.persistence.kafka.{EventDecoder, Event}

import com.typesafe.config.ConfigFactory

import kafka.serializer.Decoder
import kafka.utils.VerifiableProperties

class JournalEventDecoder(props: VerifiableProperties) extends Decoder[Event] {
  // FIXME: how to shutdown actor system (?)
  val config = ConfigFactory.parseString(props.getString("akka.analytics.serializer.config"))
  val system = ActorSystem("EventDecoder", config)
  val decoder = new EventDecoder(system)

  override def fromBytes(bytes: Array[Byte]): Event =
    decoder.fromBytes(bytes)
}
