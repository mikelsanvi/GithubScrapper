package org.mikel.githubscrapper.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.mikel.githubscrapper.{Config, GithubRepository}
import org.mikel.githubscrapper.RepositoriesStream._
import org.mikel.githubscrapper.actors.Recepcionist.SearchResults
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpecLike
import play.api.libs.ws.WSClient

import scala.concurrent.duration._

/**
  * Created by mikel on 20/05/16.
  */
class MasterTests extends TestKit(ActorSystem("ReceptionistTestActorySystem")) with FunSpecLike
  with MockFactory {

  var repoScrapperProbe = TestProbe()

  val word = "word"
  val wsClient = mock[WSClient]

  describe("A master actor") {
    it("should reply with a SearchFinished and stop itself when it receives a Start and there are no repositories") {
      val master = new ActorWithParent(Props(new Master(word)(wsClient) {
        override def repositoriesStream(): RepositoriesStream = Stream()
        override def repoScrapperProp(repo:GithubRepository) =  Props(new ChildProbeActor(repoScrapperProbe))
      }))

      master ! Master.Start

      master.parent.expectMsg(Recepcionist.SearchFinished(word))
      master.parent.expectNoMsg(50 millisecond )
    }

    it("should send a message for each repo if less than batch size") {
      val repos = (1 to Config.batchSize - 1).map(i => GithubRepository(i,"repo"+i)).toStream

      val master = new ActorWithParent(Props(new Master(word)(wsClient) {
        override def repositoriesStream(): RepositoriesStream = repos
        override def repoScrapperProp(repo: GithubRepository) = Props(new ChildProbeActor(repoScrapperProbe))
      }))


      master ! Master.Start

      repos.foreach(repo => repoScrapperProbe.expectMsg(RepoScrapper.SearchInRepo))

      repoScrapperProbe.expectNoMsg(50 millisecond)
    }

    it(s"should send the ${Config.batchSize} messages when the stream is bigger than the batchSize") {
      val repos = (1 to Config.batchSize + 10).map(i => GithubRepository(i,"repo"+i)).toStream

      val master = new ActorWithParent(Props(new Master(word)(wsClient) {
        override def repositoriesStream(): RepositoriesStream = repos
        override def repoScrapperProp(repo:GithubRepository) = Props(new ChildProbeActor(repoScrapperProbe))
      }))

      master ! Master.Start

      (1 to Config.batchSize).foreach(i => repoScrapperProbe.expectMsg(RepoScrapper.SearchInRepo))

      repoScrapperProbe.expectNoMsg(50 millisecond)
    }

    it("should reply with a SearchFinished when all repositories have sent results") {
      val repos = (1 to (Config.batchSize * 2 - 1)).map(i => GithubRepository(i,"repo"+i))

      val master = new ActorWithParent(Props(new Master(word)(wsClient) {
        override def repositoriesStream(): RepositoriesStream = repos.toStream
        override def repoScrapperProp(repo:GithubRepository) = Props(new ChildProbeActor(repoScrapperProbe))
      }))

      master ! Master.Start

      val (firstBatch, secondBatch) = repos.splitAt(Config.batchSize)

      repoScrapperProbe.expectMsgAllOf(500 millis,
        firstBatch.map(repo => RepoScrapper.SearchInRepo):_*)

      firstBatch.foreach(repo =>
        master ! Master.RepoResults(repo, List(repo.name)))

      master.parent.expectMsgAllOf(500 millis,
        firstBatch.map(repo => SearchResults(word, List(repo.name))):_*)

      repoScrapperProbe.expectMsgAllOf(500 millis,
        secondBatch.map(repo => RepoScrapper.SearchInRepo):_*)

      secondBatch.foreach(repo =>
        master ! Master.RepoResults(repo, List(repo.name)))
      secondBatch.foreach(repo =>
        master ! Master.RepoResults(repo, List(repo.name)))

      master.parent.expectMsgAllOf(500 millis,
        secondBatch.map(repo => SearchResults(word, List(repo.name))):_*)

      master.parent.expectMsg(100 millis, Recepcionist.SearchFinished(word))

      repoScrapperProbe.expectNoMsg(50 millisecond)
    }
  }


}
