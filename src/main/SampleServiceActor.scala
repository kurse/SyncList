import java.security.MessageDigest
import java.util.Calendar

import akka.actor.Actor
import org.mongodb.scala._
import spray.json._
import spray.routing.HttpService
import spray.routing.authentication.{BasicHttpAuthenticator, UserPassAuthenticator}
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import com.mongodb.casbah.commons.MongoDBObject
import main.KasbahMongoDriver
import main.models.MyJsonProtocol._
import main.models.{Company, User}
import org.apache.commons.codec.binary.Base64
import org.bson.types.ObjectId
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
      val cipher: Cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.ENCRYPT_MODE, keyToSpec(key))
      Base64.encodeBase64String(cipher.doFinal(value.getBytes("UTF-8")))
    }

    def decrypt(key: String, encryptedValue: String): String = {
      val cipher: Cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.DECRYPT_MODE, keyToSpec(key))
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
            user.password = Encryption.decrypt(key,pwd)
            val query = Document("username"->user.username)
            val userDBCriteria = MongoDBObject("username"->user.username)
            var userDB_S:String = ""
            var userDB: Option[User] = None
            mongoDriver.usersCollection.findOne(userDBCriteria).foreach{ x =>
              val id = x.get("_id").asInstanceOf[ObjectId].toString
              val username = x.get("username").asInstanceOf[String]
              val pwd = x.get("password").asInstanceOf[String]
              val isActive:Boolean = x.get("isActive").asInstanceOf[Boolean]
              val cUser = User(Some(id), username, pwd)
              userDB = Some(cUser)
            }
              if(userDB.get.password.equals(user.password)){
                val token = generateAddToken()
                complete(token)
              }
            else
                complete("fail")

          }
        }
      }

    } ~
    path("register"){
      respondWithMediaType(MediaTypes.`application/json`) {
        get{
          val pwdEncrypted = Encryption.encrypt(key,"xyz")
          complete(pwdEncrypted)
        } ~
        post {
          entity(as[User]) { user =>
            val pwd:String = user.password
            user.password = Encryption.decrypt(key,pwd)
            val query = Document("username"->user.username)
            val userDBCriteria = MongoDBObject("username"->user.username)
            var userDB_S:String = ""
            var userDB: Option[User] = None
            mongoDriver.usersCollection.findOne(userDBCriteria).foreach{ x =>
              val id = x.get("_id").asInstanceOf[ObjectId]
              val username = x.get("username").asInstanceOf[String]
              val pwd = x.get("password").asInstanceOf[String]
              if(x.containsField("orgID")){
                val orgId:Int = x.get("orgID").asInstanceOf[Int]
                val cUser = User(Some(id), username, pwd, Some(orgId))
                userDB = Some(cUser)
              }
              else{
                val cUser = User(Some(id), username, pwd)
                userDB = Some(cUser)
              }

            }
            if(userDB.isDefined)
              complete("exists")
            else{

              userDB = Some(user)
              if(userDB.get.orgID.isDefined){
                val company = Company(user.)
                mongoDriver.usersCollection += MongoDBObject("username" -> userDB.get.username,"password" -> userDB.get.password, "orgID" -> userDB.get.orgID.get)

              }
              else
                mongoDriver.usersCollection += MongoDBObject("username" -> userDB.get.username,"password" -> userDB.get.password)

              val token = generateAddToken()
              complete(token)

            }
          }


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
