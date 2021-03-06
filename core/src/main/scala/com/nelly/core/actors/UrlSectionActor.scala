package com.nelly.core.actors


import com.nelly.core.Formatter
import com.nelly.core.datastructures.HashMapPriorityQueue
import com.nelly.core.domain.{LogEntry, StatsMessage, TickInterval, UrlSection}
import scala.language.implicitConversions


class UrlSectionActor(stores: Seq[HashMapPriorityQueue[UrlSection]],
                      messageDispatcherActorName : String,
                      tickInterval: TickInterval
                       ) extends TickActor(tickInterval) {

  
  private[this] def formatStatuses(statuses: Map[Int, Int]) : String = {
    statuses.groupBy(a => a._1/100)
      .map(a => (s"${a._1}00s" , a._2.values.reduce(_+_)))
      .foldLeft("") {(a,b) => s"{status_bucket:${b._1}, count:${b._2}}"}
  }

  protected def dispatchMessage(urlSection: UrlSection, orderingId: String)  : Unit = {
    val avg = Formatter.contentSize(urlSection.averagePayloadSize)
    context.actorSelection(s"/user/${messageDispatcherActorName}") ! StatsMessage(
      s""" This is the section `${urlSection.name}` with the most ${orderingId} of
      |{hits: ${urlSection.hits}, average payload size: $avg, statuses: ${formatStatuses(urlSection.statuses)}}
      |""".stripMargin)
  }
  
  override def logEntryChange(logEntry: LogEntry): Unit = {
    val section = UrlSection(logEntry.urlSection, 1, Map(logEntry.status ->1), logEntry.responseSize.getOrElse(0))

    stores.foreach( store => store.getStoredVersion(section) match {
      case Some(t) => store.put(t + (1, (logEntry.status, 1), section.averagePayloadSize))
      case None => store.put(section)
    })
  }

  override def tick(): Unit = {
    stores.foreach( store => { 
      val tops = store.peekValues()
      tops.isEmpty match {
        case true => { println(s" No section exists yet")} 
        case _ => tops.foreach(urlSection => dispatchMessage(urlSection, store.orderingId()))
    }})
  }
}
