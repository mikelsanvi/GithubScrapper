package org.mikel.githubscrapper.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpecLike
import play.api.libs.ws.WSClient

import scala.concurrent.duration._

/**
  * Created by mikel on 20/05/16.
  */
class ReceptionistTests extends TestKit(ActorSystem("ReceptionistTestActorySystem")) with FunSpecLike with MockFactory {

  val probe = TestProbe()

  var c: WSClient = stub[WSClient]

  val recepcionist = system.actorOf(Props(new Recepcionist() {
    override def scrapperProps(word:String) = Props(new ChildProbeActor(probe))
    override def wsClient: WSClient = c
    override def terminate = probe.ref ! "terminated"
    override def processResult(word:String, links:Set[String]): Unit = probe.ref ! (word, links)
  }))


  describe("A receptionist actor") {
    it("should send a Start message to the master after Search message received") {
      recepcionist ! Recepcionist.Search("word")
      probe.expectMsg(Master.Start)
      probe.expectNoMsg(50 millisecond )
    }

    it("should be terminated when a search is started and then finished") {
      val word = "word"
      recepcionist ! Recepcionist.Search(word)
      recepcionist ! Recepcionist.SearchFinished(word)

      probe.expectMsg(Master.Start)
      probe.expectMsg("terminated")
      probe.expectNoMsg(50 millisecond )
    }

    it("should be terminated when 3 search are started and then finished") {
      val word1 = "word1"
      val word2 = "word2"
      val word3 = "word3"

      recepcionist ! Recepcionist.Search(word1)
      recepcionist ! Recepcionist.Search(word2)
      recepcionist ! Recepcionist.Search(word3)
      recepcionist ! Recepcionist.SearchFinished(word1)
      recepcionist ! Recepcionist.SearchFinished(word2)
      recepcionist ! Recepcionist.SearchFinished(word3)

      probe.expectMsg(Master.Start)
      probe.expectMsg(Master.Start)
      probe.expectMsg(Master.Start)

      probe.expectMsg("terminated")

      probe.expectNoMsg(50 millisecond )
    }

    it("should not be terminated when 3 search are started and then 2 finished") {
      val word1 = "word1"
      val word2 = "word2"
      val word3 = "word3"

      recepcionist ! Recepcionist.Search(word1)
      recepcionist ! Recepcionist.Search(word2)
      recepcionist ! Recepcionist.Search(word3)
      recepcionist ! Recepcionist.SearchFinished(word1)
      recepcionist ! Recepcionist.SearchFinished(word2)

      probe.expectMsg(Master.Start)
      probe.expectMsg(Master.Start)
      probe.expectMsg(Master.Start)

      probe.expectNoMsg(50 millisecond )
    }

    it("should process results when a SearchResult arrives") {
      val word = "word"
      val links = Set("link1", "link2")
      recepcionist ! Recepcionist.SearchResults(word,links)

      probe.expectMsg((word,links))

      probe.expectNoMsg(50 millisecond )
    }

    it("should close the wsClient when it is stopped") {
      system.stop(recepcionist)
      Thread.sleep(10)
      (c.close _).verify()
    }
  }
}
