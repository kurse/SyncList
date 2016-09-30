resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/maven-releases/"

resolvers += "spray repo" at "http://repo.spray.io"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.0")

logLevel := Level.Warn