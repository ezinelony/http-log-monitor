package com.nelly.core.actors


import akka.actor.{Actor, ActorLogging, ActorSystem}
import com.nelly.core.domain.{AlertMessage, Message, StatsMessage}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait MessageDispatcherActor extends Actor

class ConsoleMessageDispatcherActor(screenSize: Int = 25)(implicit system: ActorSystem) 
  extends MessageDispatcherActor with ActorLogging {
   
   val maxScreenMessages = new Array[Message](screenSize)
   var pointer = 0
   var maxScreenMessageList : Seq[Message] = Seq[Message]()

  private[this] def addMessage(m: Message) : Unit = {
    maxScreenMessageList = if(maxScreenMessageList.length  < screenSize) 
      maxScreenMessageList ++ Seq[Message](m) 
    else
      maxScreenMessageList.tail ++ Seq[Message](m)
  }
  
  override def receive: Receive =  {
     case (message: AlertMessage) => Future {
       println(s"\u001B[31m${message.message}\033[0m")
       println()
       maxScreenMessageList.reverse.foreach( b => {
         if(b.isInstanceOf[AlertMessage]) 
           println(s"\u001B[31m${b.message} \033[1;30m --history \033[0m")
         else  println(b.message)
       })
       addMessage(message)
     }
     case (message: StatsMessage) => Future {
          println(message.message)
          addMessage(message)
     }
   }
 }
