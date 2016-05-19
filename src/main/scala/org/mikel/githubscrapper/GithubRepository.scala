package org.mikel.githubscrapper

/**
  * Created by mikel on 17/05/16.
  */
case class GithubRepository(id:Int,  name:String) {
  lazy val branchesUrl:String = s"https://api.github.com/repos/$name/branches"

  def branchTreeUrl(branch:String):String = s"https://github.com/$name/tree/$branch"

}
