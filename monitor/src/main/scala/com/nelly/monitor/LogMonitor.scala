package com.nelly.monitor


import java.nio.file.{StandardWatchEventKinds => EventType}
//import java.util.logging.Logger

import akka.actor.{ActorLogging, ActorSystem}
import better.files.FileWatcher._
import better.files._
import com.nelly.core.domain.LogEntry

class LogMonitor(logFile: File, changeObserversRootPath: String)(
  implicit logEntryParser: LogEntryParser[LogEntry], actorSystem: ActorSystem
  )
  extends FileWatcher(logFile, false) with ActorLogging {

  var logPointer = 0
  
  println(s"... logfile ${logFile} Monitoring begins...")
  sendNotifications(logFile)
  
  self !  when(events = EventType.ENTRY_MODIFY) {
    case (EventType.ENTRY_MODIFY, file) if !file.isDirectory => {
      sendNotifications(file)
    }
    case _ => //ignore
  }
  
  private[this] def sendNotifications(logEntry: LogEntry):Unit = {
    //val path =    context.actorSelection(s"/user/${changeObserversRootPath}/*").toSerializationFormat
    //this.log.info(s"sending change event to listeners. ${path} .")
    context.actorSelection(s"/user/${changeObserversRootPath}/*") ! logEntry
  }
  

  private[this] def sendNotifications(file: File) : Unit = synchronized {
    val lines = file.lineIterator
    //logPointer = if(logPointer > lines.length) 0 else logPointer // if pointer > size? reset pointer to zero
    val iterator = lines.drop(logPointer)
    for (str <- iterator) {
      logEntryParser.parse(str) match {
        case Some(logEntry) => sendNotifications(logEntry)
        case None => {//log failure to parse

        }
      }
      logPointer +=1
    }
  }
}
