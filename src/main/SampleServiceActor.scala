import java.math.BigInteger
import java.security.MessageDigest
import java.util.Calendar
import org.json.JSONObject
import akka.actor.Actor
import org.mongodb.scala._
import spray.json._
import spray.routing.HttpService
import spray.routing.authentication.{BasicHttpAuthenticator, UserPassAuthenticator}
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.xml.bind.DatatypeConverter

import com.mongodb.casbah.commons.MongoDBObject
import main.KasbahMongoDriver
import main.models.MyJsonProtocol._
import main.models.{Company, User}
import org.apache.commons.codec.binary.Base64
import org.bson.types.ObjectId

import scala.collection.mutable.ListBuffer
import scala.util.Random
class SampleServiceActor extends Actor with SampleRoute {
  def actorRefFactory = context
  def receive = runRoute(route)
}

trait SampleRoute extends HttpService {
  import spray.httpx.SprayJsonSupport._
  import spray.http.MediaTypes
  import main.BearerTokenGenerator

  val key: String = "7AA38A614149293C80FFD1EB95FD0225"
  val tokensMap:List[String] = List()
  val tokens = scala.collection.mutable.Map[String, Long]()

  def getExpiration(id:String):Long = {
    return tokens.get(id).get
  }
  def generateAddToken():String = {
    val seconds = Calendar.getInstance().getTimeInMillis
    val bearer:BearerTokenGenerator = new BearerTokenGenerator
    val token = bearer.generateMD5Token("testprefix")
    val expiration = seconds + 1800000
    tokens += token -> expiration
    return token
  }
  val base64 = "data:([a-z]+);base64,(.*)".r

  object Encryption {
    def encrypt(key: String, value: String): String = {
      val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
      cipher.init(Cipher.ENCRYPT_MODE, keyToSpec(key))
      Base64.encodeBase64String(cipher.doFinal(value.getBytes("UTF-8")))
    }

    def decrypt(key: String, encryptedValue: String): String = {
      val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      var iv = new Array[Byte](16)
      Random.nextBytes(iv)
      val specIv = new IvParameterSpec(iv)
      cipher.init(Cipher.DECRYPT_MODE, keyToSpec(key),specIv)
      new String(cipher.doFinal(Base64.decodeBase64(encryptedValue)))
    }
    def keyToSpec(key: String): SecretKeySpec = {
      var keyBytes: Array[Byte] = (key).getBytes("UTF-8")
      val sha: MessageDigest = MessageDigest.getInstance("SHA-1")
      keyBytes = sha.digest(keyBytes)
      keyBytes = java.util.Arrays.copyOf(keyBytes, 16)
      new SecretKeySpec(keyBytes, "AES")
    }

  }


  val mongoDriver = new KasbahMongoDriver
  val resultJson:String = ""
  val route = {
    path("auth"){
      respondWithMediaType(MediaTypes.`application/json`) {
        post {
          entity(as[User]) { user =>
            val pwd:String = user.password

//            user.password = Base64.decodeBase64()

            val query = Document("username"->user.username)
            val userDBCriteria = MongoDBObject("username"->user.username)
            var userDB_S:String = ""
            var userDB: Option[User] = None
            mongoDriver.usersCollection.findOne(userDBCriteria).foreach{ x =>
              val id = x.get("_id").asInstanceOf[ObjectId].toString
              val username = x.get("username").asInstanceOf[String]
              val pwd = x.get("password").asInstanceOf[String]
              var orgId = ""
              if(x.containsField("orgID"))
                orgId = x.get("orgID").asInstanceOf[String]
              val cUser = User(Some(id), username, pwd,Some(orgId))
              userDB = Some(cUser)
            }
            println("userdbpwd = " + userDB.get.password)
            println("userpwd = " + user.password)
//            val pwdMongo = String(BCrypt.decode_base64(userDB.get.password,10000)
              if(BCrypt.checkpw(user.password, userDB.get.password)){
                val token = generateAddToken()
                val jsonUser = userDB.toJson
                if(userDB.get.orgID.equals(""))
                  complete(jsonUser.toString() + token)
                else{
                  var company = ""
                  val companyDBCriteria = MongoDBObject("orgID"->userDB.get.orgID)
                  mongoDriver.orgsCollection.findOne(companyDBCriteria).foreach { x =>
                    company = x.toString()
                    val companyName = x.get("name").asInstanceOf[String]
                    val companyId = x.get("_id").asInstanceOf[ObjectId].toString()
                    val listId = x.get("listId").asInstanceOf[String]
                    val creatorId = x.get("CreatorId").asInstanceOf[String]
                  }
                  val jsonCompany = ""
                  complete("")
                }
              }
            else
                complete("fail")

          }
        }
      }

    } ~
    path("newGroup"){
      respondWithMediaType(MediaTypes.`application/json`) {
        post{
          entity(as[Company]){ company =>
            val companyDBCriteria = MongoDBObject("name"->company.name)
            var companyDB: Option[Company] = None
            mongoDriver.orgsCollection.findOne(companyDBCriteria).foreach{ x =>
              val id = x.get("_id").asInstanceOf[ObjectId].toString
              val name = x.get("name").asInstanceOf[String]
              val listId = x.get("listId").asInstanceOf[String]
              val creator = x.get("creatorId").asInstanceOf[String]
              val company = Company(Some(id),name, creator, Some(listId))
              companyDB = Some(company)
            }
            if(companyDB != None)
              complete("exists")
            else{
              mongoDriver.orgsCollection += MongoDBObject("name" -> company.name,"creatorId" -> company.creatorID)
              mongoDriver.orgsCollection.findOne(companyDBCriteria).foreach { x =>
                var array = ListBuffer[String]
                array += "apples"
                array += "oranges"
                array += "test"
                mongoDriver.listCollection += MongoDBObject("orgId" -> x.get("_id").asInstanceOf[ObjectId].toString, "items"-> array)
//                mongoDriver.orgsCollection += MongoDBObject("name" -> company.name,"creatorId" -> company.creatorID)
                mongoDriver.orgsCollection.findOne(MongoDBObject("orgId"->x.get("orgId"))).foreach { y =>
                  val update = MongoDBObject("$set" -> MongoDBObject("listId"->y.get("_id").asInstanceOf[ObjectId].toString()))
                  val q1 = MongoDBObject("_id"->y.get("orgId"))
                  mongoDriver.orgsCollection.findAndModify(q1,update)
                  val json:JSONObject = new JSONObject()
                  json.put("id",y.get("_id").asInstanceOf[ObjectId].toString())
                  json.put("token",generateAddToken())
                  complete(json.toString())
//                  json.
//                  complete(y.get("_id").asInstanceOf[ObjectId].toString() + generateAddToken())
                }
              }

//              val token = generateAddToken()
//              complete(token)
            }
            complete("")
          }
        }
      }
    }~
    path("register"){
      respondWithMediaType(MediaTypes.`application/json`) {
        get{
          val pwdEncrypted = Encryption.encrypt(key,"xyz")
          complete(pwdEncrypted)
        } ~
        post {
          entity(as[User]) { user =>
            val userDBCriteria = MongoDBObject("username"->user.username)
            var userDB: Option[User] = None
            mongoDriver.usersCollection.findOne(userDBCriteria).foreach{ x =>
              val id = x.get("_id").asInstanceOf[ObjectId].toString
              val username = x.get("username").asInstanceOf[String]
              val pwd = x.get("password").asInstanceOf[String]
              val pwdHashed = BCrypt.hashpw(pwd,BCrypt.gensalt())
                val cUser = User(Some(id), username, pwdHashed)
                userDB = Some(cUser)
            }
            if(userDB != None)
              complete("exists")
            else{
              userDB = Some(user)
              mongoDriver.usersCollection += MongoDBObject("username" -> userDB.get.username,"password" -> userDB.get.password)
              mongoDriver.usersCollection.findOne(userDBCriteria).foreach{ x =>
                val json:JSONObject = new JSONObject()
                json.put("id",x.get("_id").asInstanceOf[ObjectId].toString())
                complete(json.toString())
              }
              complete("error")
            }

          }
          complete("error")

        }
        }
    } ~
    path("getList"){
        post {
          headerValueByName("authToken") { token =>
            if(tokens.contains(token) && tokens.get(token).get > Calendar.getInstance().getTimeInMillis)
            {
              val query = MongoDBObject("address.street" -> "Sedgwick Ave")
              var results = ""
              val json =  mongoDriver.listCollection.find(query).foreach{ x =>

                results += x.toString
              }
              complete(results)
            }
            else
              complete("invalidToken")
          }
        }
    }
  }

}
