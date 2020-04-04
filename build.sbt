name := "jistaffbackend2"

version := "1.0"

lazy val `jistaffbackend2` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

resolvers += Resolver.mavenLocal

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(evolutions, ehcache, ws, specs2 % Test, guice,
  "ch.japanimpact" %% "jiauthframework" % "1.0-SNAPSHOT",
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "com.typesafe.play" %% "play-mailer" % "8.0.0",
  "com.typesafe.play" %% "play-mailer-guice" % "8.0.0",
  "com.pauldijou" %% "jwt-play" % "4.2.0",
  "com.github.tototoshi" %% "scala-csv" % "1.3.6"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

