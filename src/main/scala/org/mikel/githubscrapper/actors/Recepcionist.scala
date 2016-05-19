package org.mikel.githubscrapper.actors

import akka.actor.{Actor, ActorLogging, Props}
import play.api.libs.ws.ning.NingWSClient

/**
  * Created by mikel on 17/05/16.
  */
class Recepcionist extends Actor with ActorLogging {

  import Recepcionist._

  lazy val client = NingWSClient()

  def receive = process(Set())

  def process(children:Set[String]):Receive={
    case Search(word) =>
      val scrapper = context.actorOf( Props(new Master(word,client)))
      context.become(process(children + word ))
      scrapper ! Master.Start
    case SearchResults(word, links) =>
      log.info(links.mkString(s"The word $word was found in: \n","\n","\n"))
    case SearchFinished(word) =>
      log.info(s"Search of word $word has finished")
      context.become(process( children - word ))
      if(children.isEmpty)
        context.system.terminate()
  }

  override def postStop(): Unit = {
    client.close()
  }
}

object Recepcionist {
  case class Search(word:String)
  case class SearchFinished(word: String)
  case class SearchResults(word:String, links: List[String])
}
