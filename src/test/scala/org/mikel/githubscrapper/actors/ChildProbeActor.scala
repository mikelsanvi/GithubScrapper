package org.mikel.githubscrapper.actors

import akka.actor.Actor
import akka.testkit.TestProbe

/**
  * Created by mikel on 20/05/16.
  */
class ChildProbeActor(probe: TestProbe) extends Actor {

  def receive = {
    case msg: Any => probe.ref ! msg
  }
}
