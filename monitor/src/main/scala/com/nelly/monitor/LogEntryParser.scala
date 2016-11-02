package com.nelly.monitor


import com.nelly.core.domain.LogEntry
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.util.{Failure, Success, Try}

trait LogEntryParser[T <: Product] {
  def parse(entry: String) :Option[T]
}

class CommonLogFormatRegexParser extends LogEntryParser[LogEntry] {

  val datePattern = DateTimeFormat.forPattern("dd/MMM/yyyy:HH:mm:ss Z")
  val reg = "^(\\S+) (\\S+) (\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})\\] \"(\\S+ \\S+\\s*\\S*\\s*)\" (\\d{3}) (\\S+)".r
  
  override def parse(entry: String): Option[LogEntry] = entry match {
    case reg(ip, identity, userId, requestReceivedTimeString, requestUrl, status, size) => {
      Try(
        LogEntry(
          ip = ip,
          receivedTime = DateTime.parse(requestReceivedTimeString, datePattern),
          requestUrl = requestUrl,
          status = status.toInt,
          identity = identity match { case "-" | "" => None; case _ =>  Option(identity)},
          userId = userId match { case "-" | "" => None; case _ =>  Option(userId)},
          responseSize = size match { case "-" | "" => None; case _ =>  Option(size.toLong)}
        )
      ) match {
        case Success(logEntry) => Option(logEntry)
        case Failure(e) => { println(s"Failed to parse:: `$e`"); None }
      }
    }
    case _ => { println(s"Failed to parse `$entry`"); None }
  }
}

object LogRecord {

  def apply( logEntry: String)(implicit parser: LogEntryParser[LogEntry] = 
  new CommonLogFormatRegexParser) : Option[LogEntry] = parser.parse(logEntry)

}
