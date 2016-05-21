package org.mikel.githubscrapper.actors

import akka.actor.{Actor, ActorLogging, Props}
import org.mikel.githubscrapper.{Config, GithubRepository, RepositoriesStream}
import play.api.libs.ws.WSClient
import org.mikel.githubscrapper.RepositoriesStream.RepositoriesStream


/**
  * Created by mikel on 17/05/16.
  */
class Master(word:String)(implicit wsClient: WSClient) extends Actor with ActorLogging {
  import Master._

  def receive = {
    case Start =>
      val (initialBatch, remainingRepositories) = repositoriesStream().splitAt(Config.batchSize)
      if(initialBatch.isEmpty) {
        context.parent ! Recepcionist.SearchFinished(word)
        context.stop(self)
      } else {
        initialBatch.foreach( searchInRepo )
        context.become(processing(remainingRepositories, initialBatch.toSet))
      }
  }

  def processing(repositoriesStream: RepositoriesStream, ongoing:Set[GithubRepository]): Receive = {
    case RepoResults(repo,links) =>
      log.info("links received " + repo)
      context.parent ! Recepcionist.SearchResults(word, links)
      searchInNextRepository(repositoriesStream, ongoing - repo )
    case NoMatchingResults(repo) =>
      searchInNextRepository(repositoriesStream,ongoing - repo)
  }

  private def searchInNextRepository(repositoriesStream: RepositoriesStream, ongoing:Set[GithubRepository]): Unit = {
    if(!repositoriesStream.isEmpty) {
      searchInRepo(repositoriesStream.head)
      context.become(processing(repositoriesStream.tail, ongoing + repositoriesStream.head))
    } else {
      if(ongoing.isEmpty) {
        context.parent ! Recepcionist.SearchFinished(word)
        context.stop(self)
      } else
        context.become(processing(repositoriesStream, ongoing))
    }
  }

  private def searchInRepo(repo:GithubRepository): Unit ={
    val repoScrapper = context.actorOf(repoScrapperProp(repo), s"Scrapper${repo.id}$word")
    repoScrapper ! RepoScrapper.SearchInRepo
  }

  def repositoriesStream(): RepositoriesStream = RepositoriesStream(wsClient)
  def repoScrapperProp(repo:GithubRepository) = Props(new RepoScrapper(word, repo))
}

object Master {
  case object Start
  case class RepoResults(repo: GithubRepository, links: List[String])
  case class NoMatchingResults(repo: GithubRepository)
}
