package org.mikel.githubscrapper

import akka.actor.{Actor, ActorLogging}
import org.mikel.githubscrapper.FolderScrapper.ScrapFolder
import play.api.libs.ws.WSClient

/**
  * Created by mikel on 17/05/16.
  */
class FolderScrapper(word:String, wsClient: WSClient) extends Actor with ActorLogging {

  def receive = {
    case ScrapFolder(folder) =>
      sender ! RepoScrapper.FilesFound(folder, List(folder+"/fakefile.txt"))
  }
}

object FolderScrapper {
  case class ScrapFolder(folder:String)
}