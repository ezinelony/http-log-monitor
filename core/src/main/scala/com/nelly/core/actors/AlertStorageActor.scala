package com.nelly.core.actors


import akka.actor.ActorSelection
import com.nelly.core.Formatter
import com.nelly.core.domain.{AlertMessage, LastXTimeStore, LogEntry}
import org.joda.time.DateTime

import scala.language.implicitConversions


class AlertStorageActor(store: LastXTimeStore, alertThreshold: Int, alertDelayInSeconds: Int,  messageDispatcherActorName : String)
                      extends TickActor(1, "seconds") {
 
    var highTrafficBeginning : DateTime = _
    var alertIsOn = false
    var lastAlertUpdate : DateTime = _

    protected def messageDispatcherActor()  : ActorSelection = {
      context.actorSelection(s"/user/${messageDispatcherActorName}")
    }
  
    override def logEntryChange(logEntry: LogEntry): Unit = { store.incrementMillisecondsCount() }

    override def tick(): Unit = {
      store.secondsTick()
      val totalCount = store.secondsTotal()
    
      (totalCount.toDouble*60/store.storeDurationInSeconds > alertThreshold) match {
        case true if !alertIsOn => { 
          alertIsOn = true 
          highTrafficBeginning = DateTime.now()
          lastAlertUpdate = DateTime.now()
          messageDispatcherActor() !  AlertMessage(
            s"High traffic generated an alert - hits = ${totalCount}, triggered at ${highTrafficBeginning}"
          )
        }
        case true if alertIsOn => {
          if(( DateTime.now().getMillis - lastAlertUpdate.getMillis) >= alertDelayInSeconds*1000  ){
            lastAlertUpdate = DateTime.now()
            messageDispatcherActor()  !  AlertMessage(
              s"High traffic has been going on for ${Formatter.timeDiff(highTrafficBeginning, DateTime.now())} - hits = ${totalCount}")
          }
      
        }
        case false if alertIsOn => {
          alertIsOn = false
          lastAlertUpdate = DateTime.now()
          messageDispatcherActor() !  AlertMessage(
            s"High Traffic that started at ${highTrafficBeginning} has returned to normal at ${DateTime.now()}  hits = ${totalCount}")
        }

        case _ =>
      }
    }
  
    
  }
