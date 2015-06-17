package akka.analytics

import org.apache.spark.SparkContext

package object cassandra {
  implicit def journalStreamingContext(context: SparkContext): JournalSparkContext =
    new JournalSparkContext(context)
}
