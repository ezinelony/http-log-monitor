package com.nelly.core.actors


import akka.actor.{ActorLogging, Actor}
import com.nelly.core.domain.LogEntry
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

abstract class TickActor(duration: Int, durationUnit: String) extends Actor with ActorLogging {
   import context._

   val tickMessage: String = "tick"
  
   def logEntryChange(logEntry: LogEntry) :Unit
   def tick() :Unit
  
   override def preStart() = scheduleTick()
   override def postRestart(reason: Throwable) = {}

   override def receive: Receive =  {
     case (logEntry: LogEntry) => Future{ logEntryChange(logEntry: LogEntry)}
     case tickMessage => Future {
         scheduleTick()
         tick()
     }
   }

   private[this] def scheduleTick() : Unit = system.scheduler.scheduleOnce(
     Duration(duration, durationUnit), self, tickMessage
   )
   
 }
