package org.mikel.githubscrapper

import com.typesafe.config.ConfigFactory

/**
 * Created by mikelsanvicente on 4/04/16.
 */
object Config {
  private val config =  ConfigFactory.load()

  def batchSize : Int = {
    config.getInt("scrapper.batch-size")
  }

  def initialRepository: Int = {
    config.getInt("scrapper.initial-repository")
  }

}
