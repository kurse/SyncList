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

import com.mongodb.BasicDBList
import com.mongodb.casbah.Imports
import com.mongodb.casbah.commons.MongoDBObject
import main.KasbahMongoDriver
import main.models.MyJsonProtocol._
import main.models.{Company, User}
import org.apache.commons.codec.binary.Base64
import org.bson.types.ObjectId
import com.mongodb.casbah.query.Imports._
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
            val pwd: String = user.password

            //            user.password = Base64.decodeBase64()
            var hasOrg = false
            val query = Document("username" -> user.username)
            val userDBCriteria = MongoDBObject("username" -> user.username)
            var userDB: Option[User] = None
            val jsonResponse: JSONObject = new JSONObject()
            mongoDriver.usersCollection.findOne(userDBCriteria).foreach { x =>
              val id = x.get("_id").asInstanceOf[ObjectId].toString
              val username = x.get("username").asInstanceOf[String]
              val pwd = x.get("password").asInstanceOf[String]
              var orgId = ""
              if (x.containsField("orgId")) {
                orgId = x.get("orgId").asInstanceOf[String]
                hasOrg = true
              }
              val cUser = User(Some(id), username, pwd, Some(orgId))
              userDB = Some(cUser)

            }
            if(userDB==None)
              complete("none")
            //            println("userdbpwd = " + userDB.get.password)
            //            println("userpwd = " + user.password)
            //            val pwdMongo = String(BCrypt.decode_base64(userDB.get.password,10000)
            else if (BCrypt.checkpw(user.password, userDB.get.password)) {
              val token = generateAddToken()
              if(hasOrg) {
                val company = mongoDriver.orgsCollection.findOne(MongoDBObject("_id" -> new ObjectId(userDB.get.orgID.get)))
                jsonResponse.put("company", company.get.toString)
              }
              jsonResponse.put("user", userDB.toJson.toString())
              jsonResponse.put("token", generateAddToken())

              //                val jsonUser = userDB.toJson
              //                if(userDB.get.orgID.equals(""))
              //                  complete(jsonResponse.toString())
              //                else{
              //                  var company:Company = Company(None,"","",None)
              //                  val companyDBCriteria = MongoDBObject("orgId"->userDB.get.orgID)
              //                  mongoDriver.orgsCollection.findOne(companyDBCriteria).foreach { x =>
              //                    val companyName = x.get("name").asInstanceOf[String]
              //                    val companyId = x.get("_id").asInstanceOf[ObjectId].toString()
              //                    val listId = x.get("listId").asInstanceOf[String]
              //                    val creatorId = x.get("creatorId").asInstanceOf[String]
              //                    company._id=Some(companyId)
              //                    company.ListId=Some(listId)
              //                    company.creatorId=creatorId
              //                    company.name=companyName
              //                  }
              //                  import main.models.CompanyJsonProtocol._
              //                  jsonResponse.put("company",company.toJson.toString())
              complete(jsonResponse.toString())

            }
            else
              complete("fail")
          }
        }
      }
    } ~
    path("newgroup"){
      import main.models.CompanyJsonProtocol._
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
              val json:JSONObject = new JSONObject()
              mongoDriver.orgsCollection += MongoDBObject("name" -> company.name,"creatorId" -> company.creatorId)
              mongoDriver.orgsCollection.findOne(companyDBCriteria).foreach { x =>
                var array = new ListBuffer[String]()
//                array += "Sample/Exemple"
//                array += "oranges"
//                array += "test"
                val list = mongoDriver.listCollection += MongoDBObject("orgId" -> x.get("_id").asInstanceOf[ObjectId].toString, "items"-> array)
//                mongoDriver.orgsCollection += MongoDBObject("name" -> company.name,"creatorId" -> company.creatorID)
                mongoDriver.listCollection.findOne(MongoDBObject("orgId"->x.get("_id").asInstanceOf[ObjectId].toString)).foreach { y =>
                  val update = MongoDBObject("$set" -> MongoDBObject("listId"->y.get("_id").asInstanceOf[ObjectId].toString()))
                  val q1 = MongoDBObject("_id"->x.get("_id"))
                  val companyDB = mongoDriver.orgsCollection.findAndModify(q1,update)
                  var arraySet = new ListBuffer[Imports.DBObject]()
                  val dbo = new MongoDBObject()
                  dbo.put("orgId",x.get("_id").asInstanceOf[ObjectId].toString)
                  dbo.put("listId",y.get("_id").asInstanceOf[ObjectId].toString())
                  val q2 = MongoDBObject("_id"-> new ObjectId(x.get("creatorId").asInstanceOf[String]))
                  val updateUser = MongoDBObject("$set" -> dbo)

                  val userDB = mongoDriver.usersCollection.findAndModify(q2,updateUser)
//                  println(userDB)
                  val companyDBC = Company(Some(x.get("_id").asInstanceOf[ObjectId].toString),x.get("name").asInstanceOf[String],company.creatorId,Some(y.get("_id").asInstanceOf[ObjectId].toString()))
                  val userDBC = User(Some(company.creatorId),userDB.get.get("username").asInstanceOf[String],"",Some(x.get("_id").asInstanceOf[ObjectId].toString))
                  json.put("company",companyDBC.toJson.toString)
                  json.put("user",userDBC.toJson.toString)
                  json.put("token",generateAddToken())
//                  json.
//                  complete(y.get("_id").asInstanceOf[ObjectId].toString() + generateAddToken())
                }
              }

//              val token = generateAddToken()
//              complete(token)
              complete(json.toString())

            }

          }
        }
      }
    } ~
    path("register"){
      respondWithMediaType(MediaTypes.`application/json`) {
        post {
          entity(as[User]) { user =>
            val userDBCriteria = MongoDBObject("username"->user.username)
            var userDB: Option[User] = None
            mongoDriver.usersCollection.findOne(userDBCriteria).foreach{ x =>
              val id = x.get("_id").asInstanceOf[ObjectId].toString
              val username = x.get("username").asInstanceOf[String]
              val pwd = x.get("password").asInstanceOf[String]
              val pwdHashed = BCrypt.hashpw(pwd,BCrypt.gensalt())
              println(pwdHashed)
                val cUser = User(Some(id), username, pwdHashed)
                userDB = Some(cUser)
            }
            if(userDB != None)
              complete("exists")
            else{
              val json:JSONObject = new JSONObject()
              userDB = Some(user)
              val pwdHashed = BCrypt.hashpw(userDB.get.password,BCrypt.gensalt())
              mongoDriver.usersCollection += MongoDBObject("username" -> userDB.get.username,"password" -> pwdHashed)
              mongoDriver.usersCollection.findOne(userDBCriteria).foreach{ x =>
                val newUser = User(Some(x.get("_id").asInstanceOf[ObjectId].toString()),
                  x.get("username").asInstanceOf[String],"")
                json.put("user",newUser.toJson.toString())
              }
              complete(json.toString())
            }

          }

        }
        }
    } ~
      path("addUserGroup") {
        respondWithMediaType(MediaTypes.`application/json`) {
          post {
            entity(as[String]) { jsonStr =>
              val jsonList = new JSONObject(jsonStr)
              headerValueByName("authToken") { token =>
                if (tokens.contains(token)) {
                  val listDB = mongoDriver.usersCollection.update(MongoDBObject("username" -> jsonList.getString("userName")), $set("orgId" -> jsonList.getString("orgId").toString(),"listId" -> jsonList.getString("listId")))
                  val jsonResult: JSONObject = new JSONObject()
                  jsonResult.put("result","ok")

                  //                  val query = MongoDBObject("_id" -> new ObjectId(jsonList.getString("listId")))
//                  var results = ""
//                  val json = mongoDriver.listCollection.findOneByID( new ObjectId(jsonList.getString("listId")))
//                  println(json)
//                  jsonResult.put("list", json.get("items").asInstanceOf[BasicDBList].toString)

                  if (tokens.get(token).get < Calendar.getInstance().getTimeInMillis) {
                    tokens.remove(token)
                    val newToken = generateAddToken()
                    jsonResult.put("token", newToken)
                  }
                  else {
                    tokens.remove(token)
                    val expiration = Calendar.getInstance().getTimeInMillis + 180000
                    tokens += token -> expiration
                  }
                  complete(jsonResult.toString())
                }
                else
                  complete("invalidToken")
              }
            }
          }
        }
      }~
    path("disconnect"){
      post{
        entity(as[String]){token =>
          tokens.remove(token)
        complete("disconnected")
        }
      }
    } ~
    path("addItem") {
      respondWithMediaType(MediaTypes.`application/json`) {
        post {
          entity(as[String]) { jsonStr =>
            val jsonList = new JSONObject(jsonStr)
            headerValueByName("authToken") { token =>
              if (tokens.contains(token)) {
                val listDB = mongoDriver.listCollection.update(MongoDBObject("_id" -> new ObjectId(jsonList.getString("listId"))), $addToSet("items" -> jsonList.getString("item")))

                val query = MongoDBObject("_id" -> new ObjectId(jsonList.getString("listId")))
                var results = ""
                val jsonResult: JSONObject = new JSONObject()
                val json = mongoDriver.listCollection.findOneByID( new ObjectId(jsonList.getString("listId")))
                println(json)
                jsonResult.put("list", json.get("items").asInstanceOf[BasicDBList].toString)

                if (tokens.get(token).get < Calendar.getInstance().getTimeInMillis) {
                  tokens.remove(token)
                  val newToken = generateAddToken()
                  jsonResult.put("token", newToken)
                }
                else {
                  tokens.remove(token)
                  val expiration = Calendar.getInstance().getTimeInMillis + 180000
                  tokens += token -> expiration
                }
                complete(jsonResult.toString())
              }
              else
                complete("invalidToken")
            }
          }
        }
      }
    }~
      path("removeItem") {
        respondWithMediaType(MediaTypes.`application/json`) {
          post {
            entity(as[String]) { jsonStr =>
              val jsonList = new JSONObject(jsonStr)
              headerValueByName("authToken") { token =>
                if (tokens.contains(token)) {
                  val listDB = mongoDriver.listCollection.update(MongoDBObject("_id" -> new ObjectId(jsonList.getString("listId"))), $pull("items" -> jsonList.getString("item")))

                  val query = MongoDBObject("_id" -> new ObjectId(jsonList.getString("listId")))
                  var results = ""
                  val jsonResult: JSONObject = new JSONObject()
                  val json = mongoDriver.listCollection.findOneByID( new ObjectId(jsonList.getString("listId")))
                  println(json)
                  jsonResult.put("list", json.get("items").asInstanceOf[BasicDBList].toString)

                  if (tokens.get(token).get < Calendar.getInstance().getTimeInMillis) {
                    tokens.remove(token)
                    val newToken = generateAddToken()
                    jsonResult.put("token", newToken)
                  }
                  else {
                    tokens.remove(token)
                    val expiration = Calendar.getInstance().getTimeInMillis + 180000
                    tokens += token -> expiration
                  }
                  complete(jsonResult.toString())
                }
                else
                  complete("invalidToken")
              }
            }
          }
        }
      } ~
    path("getList"){
        post {
          entity(as[String]) { jsonClientStr =>
            val jsonClient = new JSONObject(jsonClientStr)
            headerValueByName("authToken") { token =>
              if (tokens.contains(token)) {
                val query = MongoDBObject("_id" -> new ObjectId(jsonClient.getString("listId")))
                var results = ""
                val jsonResult: JSONObject = new JSONObject()
                val json = mongoDriver.listCollection.find(query).foreach { x =>
                  jsonResult.put("list", x.get("items").asInstanceOf[BasicDBList].toString)
                }
                if (tokens.get(token).get < Calendar.getInstance().getTimeInMillis) {
                  tokens.remove(token)
                  val newToken = generateAddToken()
                  jsonResult.put("token", newToken)
                }
                else {
                  tokens.remove(token)
                  val expiration = Calendar.getInstance().getTimeInMillis + 180000
                  tokens += token -> expiration
                }
                complete(jsonResult.toString())
              }
              else
                complete("invalidToken")
            }
          }
        }

        }
    }

}
