package org.mikel.githubscrapper

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import com.lambdaworks.jacks.JacksMapper
import play.api.libs.ws.WSClient

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by mikel on 17/05/16.
  */
class RepoScrapper(wsClient: WSClient, word: String,repo:GithubRepository) extends Actor with ActorLogging{

  import RepoScrapper._

  val folderScrapper = context.actorOf(Props(new FolderScrapper(word,wsClient)), s"${repo.id}folderScrapper$word")

  def receive = {
    case SearchInRepo =>
      wsClient.url(repo.branchesUrl).get().onComplete {
        case Success(response) =>
          val folders = JacksMapper.readValue[List[Map[String, Any]]](response.body).
            map(json => repo.branchTreeUrl(json.get("name").get.asInstanceOf[String]))
          if(folders.isEmpty)
            sendResponse(List())
          else {
            folders.foreach(folder =>  folderScrapper ! FolderScrapper.ScrapFolder(folder))
            context.become(processing(List(), folders.toSet))
          }
        case Failure(ex) =>
          log.error(ex, s"Error getting branches of ${repo.name}")
          sendResponse(List())
      }
  }

  def sendResponse(files:List[String]): Unit = {
    if(files.isEmpty)
      context.parent ! Master.NoMatchingResults(repo)
    else
      context.parent ! Master.RepoResults(repo, files)
    context.stop(self)
  }

  def processing(files:List[String], folders:Set[String]): Receive = {
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
  case class FilesFound(folder:String, files:List[String])
}