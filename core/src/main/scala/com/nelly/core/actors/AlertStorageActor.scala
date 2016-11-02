package com.nelly.core.actors


import akka.actor.ActorSelection
import com.nelly.core.Formatter
import com.nelly.core.domain.{TickInterval, AlertMessage, LastXTimeStore, LogEntry}
import org.joda.time.DateTime

import scala.language.implicitConversions


class AlertStorageActor(store: LastXTimeStore, 
                        alertThreshold: Int,
                        alertDelayInSeconds: Int,
                        messageDispatcherActorName : String,
                        tickInterval: TickInterval = TickInterval(1, "seconds")
                         ) extends TickActor(tickInterval) {
 
    private var highTrafficBeginning : DateTime = _
    private var alertIsOn = false
    private var lastAlertUpdate : DateTime = _

    def isAlertOn() : Boolean = alertIsOn
  
    protected def messageDispatcherActor()  : ActorSelection = {
      context.actorSelection(s"/user/${messageDispatcherActorName}")
    }
  
    override def logEntryChange(logEntry: LogEntry): Unit = { store.incrementMillisecondsCount() }

    override def tick(): Unit = {
      store.secondsTick()
      val totalCount = store.secondsTotal()
      val thresholdIsCrossed = totalCount.toDouble*60/Math.max(1, store.storeDurationInSeconds) > alertThreshold

      thresholdIsCrossed match {
        case true if !alertIsOn => { 
          alertIsOn = true 
          highTrafficBeginning = DateTime.now()
          lastAlertUpdate = DateTime.now()
          messageDispatcherActor() !  AlertMessage(
            s"High traffic generated an alert - hits = ${totalCount}, triggered at ${Formatter.dateTime(highTrafficBeginning)}"
          )
        }
        case true if alertIsOn => {
          if(( DateTime.now().getMillis - lastAlertUpdate.getMillis) >= alertDelayInSeconds*1000  ){
            lastAlertUpdate = DateTime.now()
            messageDispatcherActor()  !  AlertMessage(
              s"High traffic has been going on for  ${Formatter.timeDiff(highTrafficBeginning, DateTime.now())} - hits = ${totalCount}  "
            )
          }
      
        }
        case false if alertIsOn => {
          alertIsOn = false
          lastAlertUpdate = DateTime.now()
          messageDispatcherActor() !  AlertMessage(
            s"High traffic that started at ${Formatter.dateTime(highTrafficBeginning)} has returned to normal at ${Formatter.dateTime(DateTime.now())}  hits = ${totalCount} ")
        }

        case _ =>
      }
    }
  
    
  }
