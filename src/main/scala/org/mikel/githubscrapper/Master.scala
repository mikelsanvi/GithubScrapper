package org.mikel.githubscrapper

import akka.actor.{Actor, ActorLogging, Props}
import play.api.libs.ws.WSClient

/**
  * Created by mikel on 17/05/16.
  */
class Master(word:String, wsClient: WSClient) extends Actor with ActorLogging {
  import Master._

  val repoSearcher = context.actorOf(Props(new PublicRepoIterator(wsClient)), s"publicRepoIterator$word")

  def receive = {
    case Start =>
      repoSearcher ! PublicRepoIterator.FindRepos
      context.become(findRepos())
  }

  def findRepos():Receive = {
    case RepositoriesFound(repos) =>
      if(repos.isEmpty) {
        context.parent ! Recepcionist.SearchFinished(word)
      } else {
        repos.foreach(repo => {
          val repoScrapper = context.actorOf(Props(new RepoScrapper(wsClient, word,repo)), s"Scrapper${repo.id}$word")
          repoScrapper ! RepoScrapper.SearchInRepo
        })

        context.become(processing(repos))
      }
  }

  def processing(repositories: Set[GithubRepository]): Receive = {
    case RepoResults(repo,links) =>
      context.parent ! Recepcionist.SearchResults(word, links)
      checkRepositoriesLeft(repositories, repo)
    case NoMatchingResults(repo) =>
      checkRepositoriesLeft(repositories, repo)
  }

  private def checkRepositoriesLeft(repositories:Set[GithubRepository], repo: GithubRepository): Unit = {
    val remainingRepositories = repositories - repo
    if(remainingRepositories.isEmpty) {
      context.become(findRepos())
      repoSearcher ! PublicRepoIterator.FindRepos
    } else {
      context.become(processing(remainingRepositories))
    }
  }

}

object Master {
  case object Start
  case class RepoResults(repo: GithubRepository, links: List[String])
  case class NoMatchingResults(repo: GithubRepository)
  case class RepositoriesFound(repos: Set[GithubRepository])
}
