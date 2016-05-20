package org.mikel.githubscrapper.actors

import akka.actor.{Actor, ActorLogging}
import play.api.libs.ws.WSClient

/**
  * Created by mikel on 17/05/16.
  */
class FolderScrapper(word:String)(implicit wsClient: WSClient) extends Actor with ActorLogging {

  import FolderScrapper._

  def receive = {
    case ScrapFolder(folder) =>
      sender ! RepoScrapper.FilesFound(folder, List(folder+"/fakefile.txt"))
  }
}

object FolderScrapper {
  case class ScrapFolder(folder:String)
}