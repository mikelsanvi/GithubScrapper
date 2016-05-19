package org.mikel.githubscrapper

import akka.actor.{Actor, ActorLogging, Props}
import com.lambdaworks.jacks.JacksMapper
import play.api.libs.ws.WSClient

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by mikel on 17/05/16.
  */
class Master(word:String, wsClient: WSClient) extends Actor with ActorLogging {
  import Master._

  val CONCURRENT_SCRAPPINGS = 5

  def receive = {
    case Start =>
      val (initialBatch, remainingRepositories) = RepositoriesStream().splitAt(CONCURRENT_SCRAPPINGS)

      initialBatch.foreach( repo => {
        val repoScrapper = context.actorOf(Props(new RepoScrapper(wsClient, word, repo)), s"Scrapper${repo.id}$word")
        repoScrapper ! RepoScrapper.SearchInRepo
      })

      context.become(processing(remainingRepositories))
  }

  def processing(repositories: Stream[GithubRepository]): Receive = {
    case RepoResults(repo,links) =>
      context.parent ! Recepcionist.SearchResults(word, links)
      searchInNextRepository(repositories)
    case NoMatchingResults(repo) =>
      searchInNextRepository(repositories)
  }

  def searchInNextRepository(repositories: Stream[GithubRepository]): Unit = {
    if(!repositories.isEmpty) {
      val repo = repositories.head
      val repoScrapper = context.actorOf(Props(new RepoScrapper(wsClient, word, repo)), s"Scrapper${repo.id}$word")
      repoScrapper ! RepoScrapper.SearchInRepo
      context.become(processing(repositories.tail))
    } else {
      context.parent ! Recepcionist.SearchFinished(word)
      context.stop(self)
    }
  }

  object RepositoriesStream {
    val FIRST_REPO_TO_CHECK = 59000000

    def apply() = build(FIRST_REPO_TO_CHECK)

    private def build(from:Int): Stream[GithubRepository] = {
      val reposFuture = wsClient.url("https://api.github.com/repositories?since=" + from).get()
      val promise = Promise[Stream[GithubRepository]]()

      reposFuture.onSuccess {
        case response =>
          val body = JacksMapper.readValue[List[Map[String, Any]]](response.body)

          val repositories = for {
            json <- body
            id <- json.get("id")
            owner <- json.get("owner")
            name <- json.get("full_name")
          } yield(GithubRepository(id.asInstanceOf[Int],name.asInstanceOf[String]))

          promise.success(repositories.toStream #::: build( repositories.map(_.id).max+1))
      }

      reposFuture.onFailure {
        case error =>
          log.error("Error retrieving repos", error)
          promise.success(Stream())
      }
      Await.result(promise.future, 10 seconds)
    }
  }
}

object Master {
  case object Start
  case class RepoResults(repo: GithubRepository, links: List[String])
  case class NoMatchingResults(repo: GithubRepository)
}
