package com.expedia.gps.geo.reactive101.scala.client

import akka.actor.ActorSystem
import akka.util.Timeout
import com.expedia.gps.geo.reactive101.client.`type`.SimpleResponse
import com.typesafe.config.{Config, ConfigFactory}
import spray.client.pipelining._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

/**
 *
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
class SprayRESTClient(implicit ex: ExecutionContext, system: ActorSystem) extends ScalaRESTClient {
  implicit val config: Config = ConfigFactory.load()

  override def call(host: String, url: String): SimpleResponse = {
    val future = callAsync(host, url)
    Await.result(future, Duration.Inf)
  }

  override def callAsync(host: String, url: String): Future[SimpleResponse] = {
    implicit val timeout: Timeout = new Timeout(30 seconds)
    val clientPipeline = sendReceive
    clientPipeline {
      Get(s"http://$host$url")
    } map {
      case resp if resp.status.intValue == 200 =>
        new CallSuccess(resp.entity.asString)
      case resp =>
        new CallFailure(resp.status.intValue, resp.entity.asString)
    }
  }
}
