package akka.analytics.cassandra

import akka.analytics.cassandra.ProcessingSpec._
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext}

class StreamProcessingSpec extends ProcessingSpec[StreamingContext] {
  override val underTest: StreamingContext = new StreamingContext(sparkConfig, Seconds(1))

  override def execute(sc: StreamingContext): Array[(JournalKey, Any)] = {
    val rdd: RDD[(JournalKey, Any)] = underTest.eventTable().cache()
    rdd.sortByKey().collect()
  }

  override protected def afterAll(): Unit = {
    underTest.stop()
    super.afterAll()
  }
}
