package main.models
import org.bson.types.ObjectId

case class Company(_id:Option[String] = None, name: String, creatorID: String, ListID:Option[String])
import spray.json.{DefaultJsonProtocol, _}


object CompanyJsonProtocol extends DefaultJsonProtocol {
  implicit val userJsonFormat = DefaultJsonProtocol.jsonFormat4(Company.apply)
}