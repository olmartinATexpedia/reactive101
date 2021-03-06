package com.expedia.gps.geo.reactive101.scala.akka

import akka.actor._
import akka.routing.RoundRobinPool
import com.expedia.gps.geo.reactive101.client.`type`.SimpleResponse
import com.expedia.gps.geo.reactive101.scala.client.{CallFailure, CallSuccess, DispatchRESTClient}
import org.json4s.{MappingException, DefaultFormats, Formats, JValue}
import org.json4s.jackson.JsonMethods._
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, ExecutionContext}

/**
  * An advanced example using actors pool.
  */
object AdvancedAkka1_ActorPool {

  val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
  rootLogger.setLevel(ch.qos.logback.classic.Level.OFF)

  val system = ActorSystem("AKKA_test")
  implicit val executionContext: ExecutionContext = system.dispatcher
  val restClient = new DispatchRESTClient()


  def main(args: Array[String]) {
    val featureAnalyzer = system.actorOf(Props[FeatureAnalyzer], "featureAnalyzer")
    system.actorOf(RoundRobinPool(20).props(Props[GaiaCallActor]), "GaiaCallActor")
    featureAnalyzer ! CheckFeature(179898)
  }

  class FeatureAnalyzer() extends Actor {
    def receive = {

      case CheckFeature(id) =>
        val featureAnalyzer = system.actorOf(Props[ChildOfAnalyzer], s"ChildOfAnalyzer$id")
        featureAnalyzer ! FindChildOf(id)

      case HierarchyFound(feature) =>
        println(s"hierarchy for ${feature.name}\n${feature.mkString(0)}")
        println(s"Nb features found: ${feature.nbElements}")
        context.system.shutdown()

    }
  }

  class ChildOfAnalyzer() extends Actor {
    var feature: FeatureInfo = _
    var childrenOf = Seq[FeatureInfoFull]()
    var originalSender: ActorRef = _

    def receive = {

      case FindChildOf(id) =>
        originalSender = sender()
        val gaiaCallActor = context.actorSelection("/user/GaiaCallActor")
        gaiaCallActor ! CallGaia(id)

      case f: FeatureInfo =>
        if (f.childrenOfIds.isEmpty)
          originalSender ! HierarchyFound(FeatureInfoFull(f.id, f.name, Nil))
        else {
          feature = f
          for {
              id <- f.childrenOfIds
              featureAnalyzer = context.actorOf(Props[ChildOfAnalyzer], s"ChildOfAnalyzer$id")
          } featureAnalyzer ! FindChildOf(id)
        }

      case HierarchyFound(childOf) =>
        childrenOf +:= childOf
        if (childrenOf.size == feature.childrenOfIds.size) {
          println(s"Finish ${feature.id}")
          originalSender ! HierarchyFound(FeatureInfoFull(feature.id, feature.name, childrenOf))
        }
        else
          println(s"Waiting for ${feature.childrenOfIds.size - childrenOf.size} to finish ${feature.id}")
    }
  }

  class GaiaCallActor() extends Actor {
    def receive = {

      case CallGaia(id) =>
        val s = sender()
        val featureInfo = Await.result(extractChildOf(id), Duration.Inf)
        s ! featureInfo
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
          val featureIds = json \ "links" \ "atlas" \ "isParentOf" \ "featureId"
          val ids = featureIds.extractOpt[Seq[String]].getOrElse(featureIds.extract[String] :: Nil).map(_.toLong)
          FeatureInfo(id, name, ids)
        } catch {
          case e: Exception => e.printStackTrace()
            FeatureInfo(id, "?", Nil)
        }

      case CallFailure(statusCode, content) =>
        System.err.println(s"Failed to call for $id - $statusCode - $content")
        FeatureInfo(id, "?", Nil)
    }
  }

  def extractChildOf(id: Long) : Future[FeatureInfo] = {
    restClient
      .callAsync("gaia.uat.karmalab.net:8100", s"/features/$id?cid=1&apk=1&verbose=3")
      .map(extractFeatureInfo(id, _))
  }

  case class FeatureInfo(id: Long, name: String, childrenOfIds: Seq[Long] = Nil)
  case class FeatureInfoFull(id: Long, name: String, childrenOf: Seq[FeatureInfoFull] = Nil) {
    def mkString(depth: Int) : String = {
      val tabs = "\t" * depth
      tabs + "|" + name + "\n" + childrenOf.map(_.mkString(depth + 1)).mkString("")
    }
    def nbElements: Int = 1 + childrenOf.map(_.nbElements).sum
  }

  case class CheckFeature(featureId: Long)
  case class FindChildOf(featureId: Long)
  case class HierarchyFound(feature: FeatureInfoFull)
  case class CallGaia(featureId: Long)
}
