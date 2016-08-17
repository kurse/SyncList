package main.models

import org.bson.types.ObjectId

case class User(_id:Option[ObjectId] = None, username: String, var password: String, var orgID : Option[Int] = None)
import spray.json.{DefaultJsonProtocol, _}


object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val userJsonFormat = DefaultJsonProtocol.jsonFormat4(User.apply)
}