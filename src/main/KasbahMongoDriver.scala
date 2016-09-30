package main

/**
  * Created by root on 8/11/16.
  */
class KasbahMongoDriver {
  import com.mongodb.casbah.Imports._
  val mongoClient = MongoClient("127.0.0.1", 27017)
  val db = mongoClient("HeySyncList")
  val usersCollection = db("users")
  val listCollection = db("ListSync")
  val orgsCollection = db("orgs")

}
