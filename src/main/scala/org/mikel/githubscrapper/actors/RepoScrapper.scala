package org.mikel.githubscrapper.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import com.lambdaworks.jacks.JacksMapper
import org.mikel.githubscrapper.GithubRepository
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by mikel on 17/05/16.
  */
class RepoScrapper(word: String,repo:GithubRepository)(implicit wsClient: WSClient) extends Actor with ActorLogging{

  import RepoScrapper._

  val folderScrapper = context.actorOf(folderScrapperProps(),
    s"${repo.id}folderScrapper$word")

  def receive = {
    case SearchInRepo =>
      getBranches().onComplete {
        case Success(response) =>
          try {
            val folders = JacksMapper.readValue[List[Map[String, Any]]](response.body).
              map(json => repo.branchTreeUrl(json.get("name").get.asInstanceOf[String]))
            if (folders.isEmpty)
              sendResponse(Set())
            else {
              folders.foreach(folder => folderScrapper ! FolderScrapper.ScrapFolder(folder))
              context.become(processing(Set(), folders.toSet))
            }
          }catch {
            case ex:Throwable =>
              log.error(ex, s"Error getting branches of ${repo.name}")
              sendResponse(Set())
          }
        case Failure(ex) =>
          log.error(ex, s"Error getting branches of ${repo.name}")
          sendResponse(Set())
      }
  }

  def getBranches(): Future[WSResponse]= wsClient.url(repo.branchesUrl).get()
  def folderScrapperProps() = Props(new FolderScrapper(word)).withRouter(RoundRobinPool(5))

  def sendResponse(files:Set[String]): Unit = {
    if(files.isEmpty)
      context.parent ! Master.NoMatchingResults(repo)
    else
      context.parent ! Master.RepoResults(repo, files)
    context.stop(self)
  }

  def processing(files:Set[String], folders:Set[String]): Receive = {
    case FilesFound(folder, newFiles) =>
      val remainingFolders = folders - folder
      if(remainingFolders.isEmpty)
        sendResponse(files ++ newFiles)
      else
        context.become(processing(files ++ newFiles, remainingFolders))
  }
}

object RepoScrapper {
  case object SearchInRepo
  case class FilesFound(folder:String, files:Set[String])
}