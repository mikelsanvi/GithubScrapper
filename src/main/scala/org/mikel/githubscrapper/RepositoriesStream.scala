package org.mikel.githubscrapper

import com.lambdaworks.jacks.JacksMapper
import org.slf4j.LoggerFactory
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by mikel on 19/05/16.
  */
object RepositoriesStream {

  type RepositoriesStream = Stream[GithubRepository]

  lazy val wsClient = NingWSClient()

  val FIRST_REPO_TO_CHECK = 59000000

  val log = LoggerFactory.getLogger(getClass)

  def apply() = build(FIRST_REPO_TO_CHECK)

  private def build(from:Int): RepositoriesStream = {
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
