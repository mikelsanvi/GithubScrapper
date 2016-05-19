package org.mikel.githubscrapper

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import play.api.libs.ws.WSClient

/**
  * Created by mikel on 17/05/16.
  */
class Master(word:String, wsClient: WSClient) extends Actor with ActorLogging {
  import Master._

  val repoSearcher = context.actorOf(Props(new PublicRepoIterator(wsClient)), s"publicRepoIterator$word")

  def receive = findRepos(List())

  def findRepos(filesFound:List[String]):Receive = {
    case Start =>
      repoSearcher ! PublicRepoIterator.FindRepos
    case RepositoriesFound(repos) =>
      if(repos.isEmpty) {
        context.parent ! Recepcionist.SearchFinished(word, filesFound)
      } else {
        repos.foreach(repo => {
          val repoScrapper = context.actorOf(Props(new RepoScrapper(wsClient, word,repo)), s"Scrapper${repo.id}$word")
          repoScrapper ! RepoScrapper.SearchInRepo
        })

        context.become(processing(filesFound, repos))
      }
  }

  def processing(filesFound:List[String], repositoriesLeft:Set[GithubRepository]): Receive = {
    case RepoScrapped(repo,filesThatMatches) =>
      log.info(filesThatMatches.mkString(s"The word $word was found in: \n","\n","\n"))
      val rest = repositoriesLeft - repo
      if(rest.isEmpty) {
        context.become(findRepos(filesFound ++ filesThatMatches))
        repoSearcher ! PublicRepoIterator.FindRepos
      } else {
        context.become(processing(filesFound ++ filesThatMatches,  rest))
      }
  }
}

object Master {
  case object Start
  case class RepoScrapped(repo: GithubRepository, filesThatMatches: List[String])
  case class RepositoriesFound(repos: Set[GithubRepository])
}
