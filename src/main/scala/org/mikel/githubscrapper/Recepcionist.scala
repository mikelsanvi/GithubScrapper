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

  def receive = process(Map())

  def process(children:Map[String,ActorRef]):Receive={
    case Search(word) =>
      val scrapper = context.actorOf( Props(new Master(word,client)))
      context.become(process(children + (word -> scrapper)))
      scrapper ! Start
    case SearchResults(word, links) =>
      log.info(links.mkString(s"The word $word was found in: \n","\n","\n"))
    case SearchFinished(word) =>
      context.stop(children(word))
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
