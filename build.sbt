organization := "org.mikel"
name := "GithubScrapper"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ws" % "2.4.6",
  "com.typesafe.akka" %% "akka-actor" % "2.4.3",
  "com.lambdaworks" %% "jacks" % "2.5.2",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.3" % "test",
  "org.scalamock" %% "scalamock-core" % "3.2.2" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"

)