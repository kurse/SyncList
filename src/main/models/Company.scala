package main.models
import org.bson.types.ObjectId

case class Company(var _id:Option[String] = None,var name: String,var creatorId: String,var listId:Option[String]=None)
import spray.json.{DefaultJsonProtocol, _}


object CompanyJsonProtocol extends DefaultJsonProtocol {
  implicit val companyJsonFormat = DefaultJsonProtocol.jsonFormat4(Company.apply)
}