name := "JIStaffBackend"
 
version := "1.0" 
      
lazy val `jistaffbackend` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/releases/"
resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice,
  "org.reactivemongo" %% "reactivemongo" % "0.12.6",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.6-play26"
)
libraryDependencies += "com.auth0" % "java-jwt" % "3.3.0" // For json web token validation
libraryDependencies += "com.auth0" % "jwks-rsa" % "0.3.0" // For json web token public key finding


unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

      