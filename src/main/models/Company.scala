package main.models
import org.bson.types.ObjectId

case class Company(_id:Option[ObjectId] = None, name: String, creatorID: Int, ListID:Int)
import spray.json.{DefaultJsonProtocol, _}


object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val userJsonFormat = DefaultJsonProtocol.jsonFormat4(User.apply)
}