package com.expedia.gps.geo.reactive101.scala.akka

import akka.actor._
import akka.routing.RoundRobinPool
import com.expedia.gps.geo.reactive101.client.`type`.SimpleResponse
import com.expedia.gps.geo.reactive101.scala.client.{CallSuccess, DispatchRESTClient}
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Formats, JValue, MappingException}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * TODO. 
  */
object AdvancedAkkaX_DistributedAgents {

  val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
  rootLogger.setLevel(ch.qos.logback.classic.Level.OFF)

  val system = ActorSystem.create("AKKA_test")
  implicit val executionContext: ExecutionContext = system.dispatcher
  val restClient = new DispatchRESTClient()


  def main(args: Array[String]) {
    val featureAnalyzer = system.actorOf(Props[FeatureAnalyzer], "featureAnalyzer")
    system.actorOf(RoundRobinPool(6).props(Props[ChildOfAnalyzer]), "ChildOfAnalyzer")
    featureAnalyzer ! CheckFeature(319476388295869578L)
  }

  class FeatureAnalyzer() extends Actor {
    def receive = {

      case CheckFeature(id) =>
        val featureAnalyzer = context.actorSelection("/user/ChildOfAnalyzer")
        featureAnalyzer ! FindChildOf(id)

      case HierarchyFound(feature) =>
        println(s"hierachy for ${feature.name}\n${feature.mkString(0)}")
        context.system.shutdown()

    }
  }

  class ChildOfAnalyzer() extends Actor {
    var feature: FeatureInfo = _
    var childOfs = Seq[FeatureInfoFull]()
    var originalSender: ActorRef = _

    def receive = {

      case FindChildOf(id) =>
        println(s"Search $id")
        originalSender = context.sender()
        extractChildOf(id).map { f =>
          feature = f
          if (feature.childsOfIds.isEmpty)
            originalSender ! HierarchyFound(FeatureInfoFull(feature.id, feature.name, Nil))
          else {
            feature.childsOfIds.foreach { id =>
              val featureAnalyzer = context.actorSelection("/user/ChildOfAnalyzer")
              featureAnalyzer ! FindChildOf(id)
            }
          }
        }

      case HierarchyFound(childOf) =>
        childOfs = childOfs.+:(childOf)
        if (childOfs.length == feature.childsOfIds.size)
          originalSender ! HierarchyFound(FeatureInfoFull(feature.id, feature.name, childOfs))

    }
  }

  def extractFeatureInfo(id: Long, response: SimpleResponse) : FeatureInfo = {
    response match {
      case CallSuccess(content) =>
        try {
          implicit val jsonFormats: Formats = DefaultFormats
          val json: JValue = parse(content)
          val id = (json \ "id").extract[String].toLong
          val name = (json \ "name").extract[String]
          val ids: Seq[Long] = (json \ "links" \ "atlas" \\ "isParentOf").children.map(js => (js \ "featureId").extract[String].toLong)
          FeatureInfo(id, name, ids)
        } catch {
          case e: MappingException =>
            System.err.println(s"Failed to parse response for $id - $content")
            FeatureInfo(id, "?", Nil)
          case e: Exception => e.printStackTrace()
            FeatureInfo(id, "?", Nil)
        }
      case _ =>
        FeatureInfo(id, "?", Nil)
    }
  }

  def extractChildOf(id: Long) : Future[FeatureInfo] = {
    restClient
      .callAsync("gaia.uat.karmalab.net:8100", s"/features/$id?cid=1&apk=1&verbose=3")
      .map(extractFeatureInfo(id, _))
  }

  case class FeatureInfo(id: Long, name: String, childsOfIds: Seq[Long] = Nil)
  case class FeatureInfoFull(id: Long, name: String, childsOf: Seq[FeatureInfoFull] = Nil) {
    def mkString(depth: Int) : String = {
      val tabs = "\t" * depth
      tabs + "|" + name + "\n" + childsOf.map(_.mkString(depth + 1)).mkString("")
    }
  }

  case class CheckFeature(featureId: Long)
  case class FindChildOf(featureId: Long)
  case class HierarchyFound(feature: FeatureInfoFull)
}
