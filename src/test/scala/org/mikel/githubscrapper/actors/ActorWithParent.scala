package org.mikel.githubscrapper.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.TestProbe

/**
  * Created by mikel on 20/05/16.
  */
class ActorWithParent(props:Props)(implicit system:ActorSystem) {

  val parent = TestProbe()

  val self = system.actorOf(Props(new ParentActor()))

  class ParentActor extends Actor {

    lazy val child = context.actorOf(props)

    def receive = {
      case Message(m) => child ! m
      case message:Any => parent.ref ! message
    }
  }

  def !(message:Any): Unit = {
    self ! Message(message)
  }

  case class Message(m:Any)
}
