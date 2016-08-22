package models

/**
  * Created by root on 8/15/16.
  */
case class Data(_id:Option[String] = None, name: String, creatorID: String, ListID:Option[String])
import spray.json.{DefaultJsonProtocol, _}


object CompanyJsonProtocol extends DefaultJsonProtocol {
  implicit val userJsonFormat = DefaultJsonProtocol.jsonFormat4(Data.apply)
}
