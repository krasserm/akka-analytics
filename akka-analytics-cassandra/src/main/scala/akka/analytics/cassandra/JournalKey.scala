package akka.analytics.cassandra

case class JournalKey(persistenceId: String, partition: Long, sequenceNr: Long, marker: String)

object JournalKey {
  implicit object JournalKeyOrdering extends Ordering[JournalKey] {
    override def compare(x: JournalKey, y: JournalKey): Int =
      if (x.persistenceId == y.persistenceId)
        if (x.partition == y.partition)
          if (x.sequenceNr == y.sequenceNr) 0
          else math.signum(x.sequenceNr - y.sequenceNr).toInt
        else math.signum(x.partition - y.partition).toInt
      else x.persistenceId.compareTo(y.persistenceId)
  }
}
