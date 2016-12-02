import akka.actor.{ActorSystem, Props}
import akka.io.IO
import org.mongodb.scala._
import spray.can.Http

object Main extends App {
  implicit val system = ActorSystem("simple-service")
  val service = system.actorOf(Props[HeyListServiceActor], "simple-service")

  //If we're on cloud foundry, get's the host/port from the env vars
  lazy val host = Option(System.getenv("VCAP_APP_HOST")).getOrElse("0.0.0.0")
  lazy val port = Option(System.getenv("VCAP_APP_PORT")).getOrElse("8080").toInt


  IO(Http) ! Http.Bind(service, host, port = port)
}
