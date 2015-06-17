package akka.analytics

import org.apache.spark.streaming.StreamingContext

package object kafka {
  implicit def journalStreamingContext(context: StreamingContext): JournalStreamingContext =
    new JournalStreamingContext(context)
}