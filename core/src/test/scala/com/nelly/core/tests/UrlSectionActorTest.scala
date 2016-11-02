package com.nelly.core.tests


import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import com.nelly.core.actors.UrlSectionActor
import com.nelly.core.datastructures.HashMapPriorityQueue
import com.nelly.core.domain._
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import scala.concurrent.duration.Duration

class UrlSectionActorTest extends TestKit(ActorSystem("UrlSectionActorTestSpec")) with ImplicitSender with WordSpecLike with Mockito
with Matchers with BeforeAndAfterAll
{

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Url SectionActor Test" should {
    "Log entry notification merges old UrlSection object with new in store " when{
      " store finds the given url section"  in new UrlSectionActorTestRefFixture {

        when(stores(0).getStoredVersion(urlSection1)).thenReturn(Option(urlSectionStored))
        ref ! logEntry

        verify(stores(0), times(1)).put(urlSectionReplacement)
      }
    }
    "Log entry notification adds new section object in store " when{
      " store cannot find the given url section"  in new UrlSectionActorTestRefFixture {


        when(stores(0).getStoredVersion(urlSection1)).thenReturn(None)

        ref ! logEntry

        verify(stores(0), times(1)).put(urlSection1)
      }
    }
    "Dispatches stats message " when{
      " it receives a tick "  in new UrlSectionActorTestRefFixture {

        when(stores(0).peekValues).thenReturn(Seq(
          urlSectionReplacement,
        urlSectionReplacement2,
        urlSectionReplacement3
        ))
        var messages  = Seq[StatsMessage]()
      
        val m = TestActorRef(new Actor(){
            override def receive: Receive = {
              case a : StatsMessage => messages =  messages ++ Seq(a)
            }
          }, "message-dispatcher-actor-test")
        
        val result = ref ? ref.underlyingActor.tickMessage
        m.underlyingActor.context.stop(m.underlyingActor.self)
        
        assert(messages.length == 3)

        assert(messages(0).message.contains("`test1` "))
        assert(messages(1).message.contains("`test2`"))
        assert(messages(2).message.contains("`test3`"))
        
        //verify that peekValues is called once corresponding to the single message sent
        verify(stores(0), times(1)).peekValues()
      }
    }

  }


  trait UrlSectionActorTestRefFixture {
    implicit val timeout = Timeout(Duration(5 ,"seconds"))
    implicit val ordering = SectionOrdering.hitsOrdering
    val urlSection1 = UrlSection(
      name = "test1",
      hits = 1,
      statuses = Map(200 ->1),
      averagePayloadSize = 120
    )

    val urlSectionStored = UrlSection(
      name = "test1",
      hits = 12,
      statuses = Map(200 ->11, 404 -> 1),
      averagePayloadSize = 110
    )
    val urlSectionReplacement = UrlSection(
      name = "test1",
      hits = 13,
      statuses = Map(200 ->12, 404 -> 1),
      averagePayloadSize = 101
    )

    val urlSectionReplacement2 = UrlSection(
      name = "test2",
      hits = 13,
      statuses = Map(200 ->12, 404 -> 1),
      averagePayloadSize = 101
    )

    val urlSectionReplacement3 = UrlSection(
      name = "test3",
      hits = 13,
      statuses = Map(200 ->12, 404 -> 1),
      averagePayloadSize = 101
    )
    
    val messageDispatcherActorName =  "message-dispatcher-actor-test"
    val stores = Seq[HashMapPriorityQueue[UrlSection]](
      mock[HashMapPriorityQueue[UrlSection]]
    )
    val tickInterval: TickInterval = TickInterval(1, "seconds")
    val dateTime = DateTime.now()
    val logEntry = LogEntry(
    "-", dateTime,
      "GET /test1 HTTP/1.1",
      200,
      Some("nelly")
    )

    val ref =   TestActorRef(new UrlSectionActor(stores, messageDispatcherActorName, tickInterval))
  }
  
}
