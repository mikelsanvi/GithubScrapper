package org.mikel.githubscrapper.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.mikel.githubscrapper.GithubRepository
import org.mikel.githubscrapper.actors.utils.{ActorWithParentProbe, ChildProbeActor}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpecLike
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by mikel on 23/05/16.
  */
class RepoScrapperTest extends TestKit(ActorSystem("RepoScrapperTestActorySystem")) with FunSpecLike with MockFactory{

  val word = "word"
  val repo = GithubRepository(10, "test/repo")

  val wsClient = mock[WSClient]

  val folderScrapperProbe = TestProbe()

  describe("A RepoScrapper actor") {
    it("should reply with NoMatchingResults when getBranches returns a Failure") {
      val repoScrapper = new ActorWithParentProbe(Props(new RepoScrapper(word,repo)(wsClient) {
        override def getBranches(): Future[WSResponse]= {
          val promise = Promise()
          promise.tryFailure(new RuntimeException())
          promise.future
        }
        override def folderScrapperProps() = Props(new ChildProbeActor(folderScrapperProbe))
      }))

      repoScrapper ! RepoScrapper.SearchInRepo

      repoScrapper.parent.expectMsg(Master.NoMatchingResults(repo))
    }

    it("should reply with NoMatchingResults when getBranches retrieve the wrong message") {
      val repoScrapper = new ActorWithParentProbe(Props(new RepoScrapper(word,repo)(wsClient) {
        override def getBranches(): Future[WSResponse]= Future{
          val response = mock[WSResponse]
          (response.body _).expects().returns("[{\"a\":1}]")
          response
        }
        override def folderScrapperProps() = Props(new ChildProbeActor(folderScrapperProbe))
      }))

      repoScrapper ! RepoScrapper.SearchInRepo

      repoScrapper.parent.expectMsg(Master.NoMatchingResults(repo))
    }

    it("should reply with NoMatchingResults when getBranches retrieve an empty list of branches") {
      val repoScrapper = new ActorWithParentProbe(Props(new RepoScrapper(word,repo)(wsClient) {
        override def getBranches(): Future[WSResponse]= Future{
          val response = mock[WSResponse]
          (response.body _).expects().returns("[]")
          response
        }
        override def folderScrapperProps() = Props(new ChildProbeActor(folderScrapperProbe))
      }))

      repoScrapper ! RepoScrapper.SearchInRepo

      repoScrapper.parent.expectMsg(Master.NoMatchingResults(repo))
    }

    it("should send a ScrapFolder message to the FolderScrapper for each branch found and send back a RepoResults " +
      "when all folder searches have been completed") {
      val repoScrapper = new ActorWithParentProbe(Props(new RepoScrapper(word,repo)(wsClient) {
        override def getBranches(): Future[WSResponse]= Future{
          val response = mock[WSResponse]
          (response.body _).expects().returns("[{\"name\":\"master1\"},{\"name\":\"master2\"},{\"name\":\"master3\"}]")
          response
        }
        override def folderScrapperProps() = Props(new ChildProbeActor(folderScrapperProbe))
      }))

      repoScrapper ! RepoScrapper.SearchInRepo

      folderScrapperProbe.expectMsgAllOf(100 millis, FolderScrapper.ScrapFolder(repo.branchTreeUrl("master1")),
        FolderScrapper.ScrapFolder(repo.branchTreeUrl("master2")),
        FolderScrapper.ScrapFolder(repo.branchTreeUrl("master3")))

      repoScrapper ! RepoScrapper.FilesFound(repo.branchTreeUrl("master1"), Set("master1file1","master1file2"))
      repoScrapper ! RepoScrapper.FilesFound(repo.branchTreeUrl("master2"), Set("master2file1","master2file2","master2file3"))
      repoScrapper ! RepoScrapper.FilesFound(repo.branchTreeUrl("master3"), Set("master3file1"))

      repoScrapper.parent.expectMsg(Master.RepoResults(repo, Set("master1file1","master1file2", "master2file1",
        "master2file2","master2file3", "master3file1")))
    }
  }
}
