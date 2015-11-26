package com.expedia.gps.geo.reactive101.server.controller

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import org.json4s.JsonDSL.WithDouble._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import org.scalatra.{ApiFormats, AsyncResult, FutureSupport, ScalatraServlet}

import scala.concurrent._

/**
 *
 * @author olmartin@expedia.com
 * @since 2015-11-12
 */
class FoodController(system: ActorSystem) extends ScalatraServlet
  with ApiFormats
  with FutureSupport
  with StrictLogging
  with JacksonJsonSupport{

  protected implicit val jsonFormats: Formats = DefaultFormats
  implicit def executor: ExecutionContext = system.dispatcher

  before() {
    contentType = formats("json")
  }

  get("/takeOrder") {
    new AsyncResult() {
      override val is: Future[_] = Future {
        blocking {
          Thread.sleep(500)
        }
        val food = if (Math.random() > 0.5d) "pizza" else "hotdog"
        val order = ("order" -> food)
        println(s"Take order: $food")
        render(order)
      }
    }
  }

  get("/preparePizza") {
    new AsyncResult() {
      override val is: Future[_] = Future {
        blocking {
          Thread.sleep(500)
        }
        val food = ("food" -> "pizza ready!")
        println("Prepare pizza")
        render(food)
      }
    }
  }

  get("/prepareHotdog") {
    new AsyncResult() {
      override val is: Future[_] = Future {
        blocking {
          Thread.sleep(500)
        }
        val food = ("food" -> "hotdog ready!")
        println("Prepare hotdog")
        render(food)
      }
    }
  }
}
