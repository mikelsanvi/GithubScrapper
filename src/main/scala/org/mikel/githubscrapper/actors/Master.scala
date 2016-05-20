package org.mikel.githubscrapper.actors

import akka.actor.{Actor, ActorLogging, Props}
import org.mikel.githubscrapper.{GithubRepository, RepositoriesStream}
import play.api.libs.ws.WSClient
import org.mikel.githubscrapper.RepositoriesStream.RepositoriesStream


/**
  * Created by mikel on 17/05/16.
  */
class Master(word:String, wsClient: WSClient) extends Actor with ActorLogging {
  import Master._

  val CONCURRENT_SCRAPPINGS = 5

  def receive = {
    case Start =>
      val (initialBatch, remainingRepositories) = RepositoriesStream(wsClient).splitAt(CONCURRENT_SCRAPPINGS)

      if(initialBatch.isEmpty) {
        context.parent ! Recepcionist.SearchFinished(word)
        context.stop(self)
      } else {
        initialBatch.foreach( searchInRepo )
        context.become(processing(remainingRepositories))
      }
  }

  def processing(repositoriesStream: RepositoriesStream): Receive = {
    case RepoResults(repo,links) =>
      context.parent ! Recepcionist.SearchResults(word, links)
      searchInNextRepository(repositoriesStream)
    case NoMatchingResults(repo) =>
      searchInNextRepository(repositoriesStream)
  }

  private def searchInNextRepository(repositoriesStream: RepositoriesStream): Unit = {
    if(!repositoriesStream.isEmpty) {
      searchInRepo(repositoriesStream.head)
      context.become(processing(repositoriesStream.tail))
    } else {
      context.parent ! Recepcionist.SearchFinished(word)
      context.stop(self)
    }
  }

  private def searchInRepo(repo:GithubRepository): Unit ={
    val repoScrapper = context.actorOf(Props(new RepoScrapper(wsClient, word, repo)), s"Scrapper${repo.id}$word")
    repoScrapper ! RepoScrapper.SearchInRepo
  }
}

object Master {
  case object Start
  case class RepoResults(repo: GithubRepository, links: List[String])
  case class NoMatchingResults(repo: GithubRepository)
}
