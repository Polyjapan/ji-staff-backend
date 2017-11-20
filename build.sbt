name := "JIStaffBackend"
 
version := "1.0" 
      
lazy val `jistaffbackend` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/releases/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )
libraryDependencies += "com.auth0" % "java-jwt" % "3.3.0" // For json web token validation
libraryDependencies += "com.auth0" % "jwks-rsa" % "0.3.0" // For json web token public key finding
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

      