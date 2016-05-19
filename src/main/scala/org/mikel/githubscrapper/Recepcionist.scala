package org.mikel.githubscrapper

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.mikel.githubscrapper.Master.Start
import play.api.libs.ws.ning.NingWSClient

/**
  * Created by mikel on 17/05/16.
  */
class Recepcionist extends Actor with ActorLogging {

  import Recepcionist._

  lazy val client = NingWSClient()

  private var children = Map[String,ActorRef]()

  def receive = {
    case Search(word) =>
      val scrapper = context.actorOf( Props(new Master(word,client)))
      children +=  (word -> scrapper)
      scrapper ! Start
    case SearchFinished(word, filesFound) =>
      context.stop(children(word))
      children = children.-(word)
      if(children.isEmpty)
        context.system.terminate()
  }

  override def postStop(): Unit = {
    //client.close()
  }
}

object Recepcionist {
  case class Search(word:String)
  case class SearchFinished(word: String, filesFound: List[String])
}
