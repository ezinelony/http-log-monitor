package com.nelly.monitor


import java.io.File
import akka.actor.{Actor, ActorSystem, Props}
import com.nelly.core.actors.{AlertStorageActor, ConsoleMessageDispatcherActor, UrlSectionActor}
import com.nelly.core.datastructures.HashMapPriorityQueue
import com.nelly.core.domain.{TickInterval, SectionOrdering, ShortTermTimeStore}
import com.nelly.util.EnvironmentalConfig


object LogMonitorApplication extends App {
  implicit val system = ActorSystem("log-monitor-system")
  implicit val logEntryParser = new CommonLogFormatRegexParser

  val shortTermStorage = new ShortTermTimeStore(EnvironmentalConfig.lastAlertCalculationDurationInSeconds)
  val maxSectionHitsStore = new HashMapPriorityQueue()(SectionOrdering.hitsOrdering) //store for sections with the most hits
  val logFile = new File(EnvironmentalConfig.logPath)

  logFile.exists() && !logFile.isDirectory match {
    case true => {
      system.actorOf(Props(
        new ConsoleMessageDispatcherActor(screenSize = EnvironmentalConfig.screenSize)), "message-dispatcher-actor"
      )

      system.actorOf( Props(new Actor() {
        context.actorOf( Props(new UrlSectionActor(
          Seq(maxSectionHitsStore
          ), "message-dispatcher-actor", TickInterval(EnvironmentalConfig.sectionTickDurationInSeconds, "seconds")
        )), "section-actor")

        context.actorOf(Props(new AlertStorageActor(
          shortTermStorage,
          EnvironmentalConfig.alertThreshold,
          EnvironmentalConfig.alertDelayInSeconds,
          "message-dispatcher-actor")
        ), "log-alert-actor")
        override def receive: Receive = { case a => {/* do nothing */} }
      }
      ), "log-listeners"
      )
      
      system.actorOf( Props(new LogMonitor(logFile.toPath, "log-listeners")), "log-monitor")
    }
    case _ => println(s"\u001B[31mNo log file, exiting ..."); println(s" ...Bye"); sys.exit(0)
  }
}
