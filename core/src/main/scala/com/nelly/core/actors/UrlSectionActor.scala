package com.nelly.core.actors


import com.nelly.core.Formatter
import com.nelly.core.datastructures.HashMapPriorityQueue
import com.nelly.core.domain.{StatsMessage, LogEntry, UrlSection}
import scala.language.implicitConversions


class UrlSectionActor(
                       stores: Seq[HashMapPriorityQueue[UrlSection]], 
                       tickDuration: Int,
                       messageDispatcherActorName : String,
                       durationUnit: String = "seconds"
                       ) extends
TickActor(tickDuration,durationUnit) {


  override def logEntryChange(logEntry: LogEntry): Unit = {
    val section = UrlSection(logEntry.urlSection, 1, Map(logEntry.status ->1), logEntry.responseSize.getOrElse(0))

    stores.foreach( store => store.getStoredVersion(section) match {
      case Some(t) => store.put(t + (1, (logEntry.status, 1), section.averagePayloadSize)) //add 1 to the totals hits for this section and merge the 
      case None => store.put(section)
    })
  }

  override def tick(): Unit = {
    stores.foreach( store => { 
      val tops = store.peekValues()
      tops.isEmpty match {
        case true => { println(s" No section exists yet")} 
        case _ => {
        tops.foreach(urlSection => {
          context.actorSelection(s"/user/${messageDispatcherActorName}") ! StatsMessage(
            s""" This is the section /${urlSection.name}/ with the most ${store.orderingId()} of
               |{hits: ${urlSection.hits}, average payload size: ${Formatter.contentSize(urlSection.averagePayloadSize)}, statuses: ${urlSection.statuses}  }
               | """.stripMargin)
        })
      }
    }})
  }
}
