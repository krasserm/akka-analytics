package akka.analytics

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext

package object cassandra {
  implicit def journalContext(
      context: SparkContext
    ): JournalContext[SparkContext, RDD] =
    new JournalContext(context)(new JournalSparkContext(context))

  implicit def journalContext(
      context: StreamingContext
    ): JournalContext[StreamingContext, RDD] =
    new JournalContext(context)(new JournalStreamingContext(context))
}
