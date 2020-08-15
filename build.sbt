


import sbt.Keys.{libraryDependencies, resolvers}

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JDebPackaging, SystemdPlugin, JavaServerAppPackaging)
  .settings(
    name := "jistaffbackend2",
    version := "1.0",
    scalaVersion := "2.13.1",

    maintainer in Linux := "Louis Vialar <louis@japan-impact.ch>",

    javaOptions in Universal ++= Seq(
      // Provide the PID file
      s"-Dpidfile.path=/dev/null",
      // s"-Dpidfile.path=/run/${packageName.value}/play.pid",

      // Set the configuration to the production file
      s"-Dconfig.file=/usr/share/${packageName.value}/conf/production.conf",

      // Apply DB evolutions automatically
      "-DapplyEvolutions.default=true"
    ),


    libraryDependencies ++= Seq(evolutions, caffeine, ws, specs2 % Test, guice,
      "ch.japanimpact" %% "jiauthframework" % "2.0-SNAPSHOT",
      "com.typesafe.play" %% "play-slick" % "5.0.0",
      "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
      "mysql" % "mysql-connector-java" % "5.1.34",
      "com.typesafe.play" %% "play-mailer" % "8.0.0",
      "com.typesafe.play" %% "play-mailer-guice" % "8.0.0",
      "com.pauldijou" %% "jwt-play" % "4.2.0",
      "ch.japanimpact" %% "ji-events-api" % "1.0-SNAPSHOT",
      "com.github.tototoshi" %% "scala-csv" % "1.3.6"
    ),

    resolvers += "Japan Impact Releases" at "https://repository.japan-impact.ch/releases",
    resolvers += "Japan Impact Snapshots" at "https://repository.japan-impact.ch/snapshots",
    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/",

    resolvers += Resolver.mavenLocal,

    scalaVersion := "2.13.1",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation"
    )
  )

