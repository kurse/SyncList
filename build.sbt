
name := "MongoTest"

version := "1.0"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(JavaAppPackaging)
resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1"
libraryDependencies += "commons-codec" % "commons-codec" % "1.10"
val akka = "2.3.9"
libraryDependencies += "org.apache.commons" % "commons-email" % "1.2"
val spray = "1.3.2"
mainClass in (Compile, run) := Some("Main")
resolvers += Resolver.url("TypeSafe Ivy releases", url("http://dl.bintray.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
libraryDependencies += "org.mongodb" %% "casbah" % "3.1.1"
libraryDependencies += "org.json" % "json" % "20090211"
resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

libraryDependencies += "me.lessis" %% "courier" % "0.1.3"
libraryDependencies ++= Seq(

  "org.scala-lang" % "scala-reflect" % "2.11.8",
  "org.scala-lang" % "scala-compiler" % "2.11.8",
  "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.4",
"org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4"
)

// https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.2"
libraryDependencies += "com.google.code.gson" % "gson" % "1.7.1"

libraryDependencies ++=
  Seq(
    // -- Logging --
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    // -- Akka --
    "com.typesafe.akka" %% "akka-testkit" % akka % "test",
    "com.typesafe.akka" %% "akka-actor" % akka,
    "com.typesafe.akka" %% "akka-slf4j" % akka,
    // -- Spray --
    "io.spray" %% "spray-routing" % spray,
    "io.spray" %% "spray-client" % spray,
    "io.spray" %% "spray-testkit" % spray % "test",
    // -- json --
    "io.spray" %% "spray-json" % "1.3.2",
    // -- config --
    "com.typesafe" % "config" % "1.2.1"
    // -- testing --
//    "org.scalatest" %% "scalatest" % "2.2.1" % "test"
  )


scalacOptions += "-deprecation"