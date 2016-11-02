package com.nelly.monitor

import java.io.File

import akka.actor.{Actor, ActorSystem, Props}
import com.nelly.core.actors.{ConsoleMessageDispatcherActor, AlertStorageActor, UrlSectionActor}
import com.nelly.core.datastructures.HashMapPriorityQueue
import com.nelly.core.domain.{SectionOrdering, ShortTermTimeStore}

import scala.util.Try


object LogMonitorApplication extends App {


  implicit val system = ActorSystem("log-monitor-system")
  implicit val logEntryParser = new CommonLogFormatRegexParser
  
  val logPath = sys.env.getOrElse("ENV_ACCESS_LOG", "/Users/nelly/Documents/Workspaces/challenges/access_log")
  val alertThreshold = Try(sys.env.getOrElse("ENV_ALERT_THRESHOLD", "10").toInt).toOption match {
    case Some(th) => th
    case _ => 10
  }

  val alertDelayInSeconds = Try(sys.env.getOrElse("ENV_ALERT_DELAY_IN_SECONDS", "60").toInt).toOption match {
    case Some(th) => th
    case _ => 60
  }
  val sectionTickDurationInSeconds = Try(sys.env.getOrElse("ENV_SECTION_TICK_DURATION_IN_SECONDS", "10").toInt).toOption match {
    case Some(th) => th
    case _ => 10
  }
  
  val lastAlertCalculationDurationInSeconds = Try(sys.env.getOrElse("ENV_ALERT_WINDOW_IN_MINUTES", "2").toInt).toOption match {
    case Some(th) => th*60
    case _ => 120
  }
  
  val screenSize =  Try(sys.env.getOrElse("ENV_SCREEN_WINDOW", "25").toInt).toOption match {
    case Some(th) => th
    case _ => 100
  }

  val shortTermStorage = new ShortTermTimeStore(lastAlertCalculationDurationInSeconds)
  
  val maxSectionHitsStore = new HashMapPriorityQueue()(SectionOrdering.hitsOrdering) //store for sections with the most hits
  val maxSectionNotFoundStore = new HashMapPriorityQueue()(SectionOrdering.notFoundOrdering)//store for  sections with the most 404s
  val maxSectionInternalServerErrorStore = new HashMapPriorityQueue()(SectionOrdering.internalServerOrdering)//store for  sections with the most 500s (499 < x  <600)
  val logFile = new File(logPath)
  
  logFile.exists() match {
    case true => {
      system.actorOf( Props( new ConsoleMessageDispatcherActor(screenSize = screenSize)), "message-dispatcher-actor")
      system.actorOf( Props(new Actor() {
        context.actorOf( Props(new UrlSectionActor(
          Seq(maxSectionHitsStore
          ),sectionTickDurationInSeconds, "message-dispatcher-actor"
        )), "section-actor")

        context.actorOf(Props(new AlertStorageActor(
          shortTermStorage,
          alertThreshold,
          alertDelayInSeconds,
          "message-dispatcher-actor")
        ), "log-alert-actor")
        override def receive: Receive = { case a => {/* do nothing */} }
      }), "log-listeners"
      )
      
      system.actorOf( Props(new LogMonitor(logFile.toPath, "log-listeners")), "log-monitor")
    }
    case _ => println(s"\u001B[31mNo log file, exiting ..."); println(s" ...Bye"); sys.exit(0)
  }
}
