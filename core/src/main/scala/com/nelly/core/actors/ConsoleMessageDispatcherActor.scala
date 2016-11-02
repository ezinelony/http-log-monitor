package com.nelly.core.actors


import akka.actor.{ActorLogging, ActorSystem}
import com.nelly.core.domain.{AlertMessage, Message, StatsMessage}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class ConsoleMessageDispatcherActor(screenSize: Int = 25)(implicit system: ActorSystem) 
  extends MessageDispatcherActor with ActorLogging {
   
   val maxScreenMessages = new Array[Message](screenSize)
   var pointer = 0

  private[this] def addMessage(m: Message) : Unit = {
    maxScreenMessages.update(pointer, m)
    pointer += (pointer + 1)%screenSize
  }
   override def receive: Receive =  {
     case (message: AlertMessage) => Future {
       println(s"\u001B[31m${message.message}")
       println()
       maxScreenMessages.filter( a => a != null).reverse.foreach( b => {
         if(b.isInstanceOf[AlertMessage]) println(s"\u001B[31m${b.message}") else  println(b.message)
       })
       addMessage(message)
     }
     case (message: StatsMessage) => Future {
          println(message.message)
          addMessage(message)
     }
   }
 }
