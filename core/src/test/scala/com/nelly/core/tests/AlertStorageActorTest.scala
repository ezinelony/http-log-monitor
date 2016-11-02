package com.nelly.core.tests

//import org.mockito.Mockito

import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import com.nelly.core.actors.AlertStorageActor
import com.nelly.core.domain.{AlertMessage, LastXTimeStore, LogEntry}
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.specs2.mock.Mockito

import scala.concurrent.duration.Duration

class AlertStorageActorTest extends TestKit(ActorSystem("AlertStorageActorTestSpec")) with ImplicitSender with WordSpecLike with Mockito
with BeforeAndAfterAll
{

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Alert Storage Actor" should {
    "calls store to increment it's milliseconds counter " when{
      " it receives a notification about a log entry"  in new AlertStorageActorRefFixture {

        when(shortTermStorage.storeDurationInSeconds).thenReturn(10)
        doNothing.when(shortTermStorage).incrementMillisecondsCount()
        ref ! logEntry

        verify(shortTermStorage, times(1)).incrementMillisecondsCount()
      }
    }
    "sends a high alert message " when{
      " average total hits is greater than threshold "  in new AlertStorageActorRefFixture {

        when(shortTermStorage.storeDurationInSeconds).thenReturn(10)
        doNothing.when(shortTermStorage).secondsTick()
        when(shortTermStorage.secondsTotal()).thenReturn(20)

        val m = TestActorRef(new Actor(){
            override def receive: Receive = {
              case a : AlertMessage => assert(a.message.contains("hits = 20"))
            }
          }, "message-dispatcher-actor-test")
        val result = ref ? ref.underlyingActor.tickMessage
        m.underlyingActor.context.stop(m.underlyingActor.self)
        
        //verify that secondsTick is called once corresponding to the single message sent
        verify(shortTermStorage, times(1)).secondsTick()
      }
    }

    "sends a return to normal alert message " when{
      " average total hits decreases below the threshold "  in new AlertStorageActorRefFixture {
        
        when(shortTermStorage.storeDurationInSeconds).thenReturn(10)
        doNothing.when(shortTermStorage).secondsTick()
        when(shortTermStorage.secondsTotal())
          .thenReturn(20)
          .thenReturn(0)

        var messages  = Seq[AlertMessage]()
        
        val m = TestActorRef(new Actor(){
          override def receive: Receive = {
            case a : AlertMessage => { messages = messages ++ Seq[AlertMessage](a)}
          }
        }, "message-dispatcher-actor-test")
        
        ref ? ref.underlyingActor.tickMessage
        ref ? ref.underlyingActor.tickMessage
        
        assert(messages.length == 2)
        assert(messages(0).message.contains("High traffic generated an alert "))
        assert(messages(1).message.contains("returned to normal"))
        
        m.underlyingActor.context.stop(m.underlyingActor.self)

        //verify that secondsTick is called twice corresponding to the two messages sent
        verify(shortTermStorage, times(2)).secondsTick()
      }
    }

    "sends a high traffic persistent alert message " when{
      " average total hits continue to be greater than the threshold while alert is on"  in new AlertStorageActorRefFixture {
        
        when(shortTermStorage.storeDurationInSeconds).thenReturn(10)
        doNothing.when(shortTermStorage).secondsTick()
        when(shortTermStorage.secondsTotal())
          .thenReturn(1)
          .thenReturn(1)
          .thenReturn(0)

        var messages  = Seq[AlertMessage]()
        val m = TestActorRef(new Actor(){
          override def receive: Receive = {
            case a : AlertMessage => { messages = messages ++ Seq[AlertMessage](a)}
          }
        }, "message-dispatcher-actor-test")
        
        ref ? ref.underlyingActor.tickMessage
        ref ? ref.underlyingActor.tickMessage
        ref ? ref.underlyingActor.tickMessage
        
        m.underlyingActor.context.stop(m.underlyingActor.self)

        assert(messages.length == 3)
        assert(messages(0).message.contains("High traffic generated an alert "))
        assert(messages(1).message.contains("High traffic has been going"))
        assert(messages(2).message.contains("returned to normal"))

        //verify that secondsTick is called thrice corresponding to the three messages sent
        verify(shortTermStorage, times(3)).secondsTick()
      }
    }
  }


  trait AlertStorageActorRefFixture {
    implicit val timeout = Timeout(Duration(5 ,"seconds"))
    val dateTime = DateTime.now()
    val logEntry = LogEntry(
    "-", dateTime,
      "GET /announce HTTP/1.1",
      200,
      Some("nelly")
    )
    val shortTermStorage = mock[LastXTimeStore]
    val ref =   TestActorRef(new AlertStorageActor(shortTermStorage, 1, 0, "message-dispatcher-actor-test"))
  }
  
}
