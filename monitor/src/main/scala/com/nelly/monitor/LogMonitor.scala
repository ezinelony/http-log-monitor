package com.nelly.monitor


import java.nio.file.{StandardWatchEventKinds => EventType}
import akka.actor.{ActorLogging, ActorSystem}
import better.files.FileWatcher._
import better.files._
import com.nelly.core.domain.LogEntry


class LogMonitor(logFile: File, changeObserversRootPath: String)(
  implicit logEntryParser: LogEntryParser[LogEntry], actorSystem: ActorSystem
  )
  extends FileWatcher(logFile, false) with ActorLogging {

  private var logPointer = 0
  
  this.log.info(s"... logfile ${logFile} Monitoring begins...")
  sendNotifications(logFile) //On start up, read whatever is in the log file and send out notifications
  
  /*
   listen to log file change event and supply a callback
   */
  self !  when(events = EventType.ENTRY_MODIFY) {
    case (EventType.ENTRY_MODIFY, file) if !file.isDirectory => sendNotifications(file)
    case _ => //ignore
  }
  
  private[this] def sendNotifications(logEntry: LogEntry):Unit = {
    context.actorSelection(s"/user/${changeObserversRootPath}/*") ! logEntry
  }
  
  private[this] def sendNotifications(file: File) : Unit = synchronized {
    val startingPointer = logPointer
    /* stream to prevent loading the entire file in memory (to prevent possible out of memory exception) */
    val lines = file.lineIterator 

    val iterator = lines.drop(logPointer)
    for (str <- iterator) {
      logEntryParser.parse(str) match {
        case Some(logEntry) => sendNotifications(logEntry)
        case None =>  this.log.error(s"Failed to parse log entry ${str}")
      }
      logPointer +=1
    }

    //if the log file gets truncated, reset pointer
    logPointer = if(0 != logPointer && startingPointer == logPointer) file.lineIterator.length else logPointer

  }
}
