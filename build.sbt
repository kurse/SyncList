name := "MongoTest"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1"
libraryDependencies += "commons-codec" % "commons-codec" % "1.10"
val akka = "2.3.9"

val spray = "1.3.2"

resolvers += Resolver.url("TypeSafe Ivy releases", url("http://dl.bintray.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
libraryDependencies += "org.mongodb" %% "casbah" % "3.1.1"
libraryDependencies ++= Seq(

  "org.scala-lang" % "scala-reflect" % "2.11.8",
  "org.scala-lang" % "scala-compiler" % "2.11.8",
  "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.4",
"org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4"
)

libraryDependencies += "com.github.t3hnar" %% "scala-bcrypt" % "2.6"
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