package org.mikel.githubscrapper

import akka.actor.{Actor, ActorLogging}
import com.lambdaworks.jacks.JacksMapper
import org.mikel.githubscrapper.Master.RepositoriesFound
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by mikel on 17/05/16.
  */
class PublicRepoIterator(wsClient: WSClient) extends Actor with ActorLogging {

  import PublicRepoIterator._

  def receive = search(59000000)

  def search(from:Int): Receive = {
    case FindRepos =>
      val me = self
      val reposFuture = wsClient.url("https://api.github.com/repositories?since="+from).get()

      reposFuture.onSuccess {
        case response =>
          val body = JacksMapper.readValue[List[Map[String, Any]]](response.body)

          val repositories = for {
            json <- body
            id <- json.get("id")
            owner <- json.get("owner")
            name <- json.get("full_name")
          } yield(GithubRepository(id.asInstanceOf[Int],name.asInstanceOf[String]))

          log.info(repositories.map(repo => repo.branchesUrl).mkString("Repositiories: \n","\n","\n"))

          context.parent ! Master.RepositoriesFound(repositories.toSet)

          context.become(search(repositories.map(_.id).max+1))
      }

      reposFuture.onFailure {
        case error =>
          log.error("Error retrieving repos", error)
          context.parent ! RepositoriesFound(Set())
      }
  }
}

object PublicRepoIterator {
  case object FindRepos
}