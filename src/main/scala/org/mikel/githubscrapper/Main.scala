package org.mikel.githubscrapper

import akka.actor.{ActorSystem, Props}

/**
  * Created by mikel on 17/05/16.
  */
object Main extends App {
  val system = ActorSystem("GithubScrapperSystem")
  val recep = system.actorOf(Props(new Recepcionist()),"Recepcionist")

  recep ! Recepcionist.Search("hola")
}
