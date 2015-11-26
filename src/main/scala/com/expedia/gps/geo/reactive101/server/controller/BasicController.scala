package com.expedia.gps.geo.reactive101.server.controller

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import org.scalatra.{FutureSupport, AsyncResult, ApiFormats, ScalatraServlet}

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.blocking

/**
 *
 * @author olmartin@expedia.com
 * @since 2015-11-12
 */
class BasicController(system: ActorSystem) extends ScalatraServlet
  with ApiFormats
  with FutureSupport
  with StrictLogging {

  implicit def executor: ExecutionContext = system.dispatcher
  var count = new AtomicInteger(0)

  get("/") {
    "It's working!"
  }

  get("/reset") {
    count.set(0)
  }

  get("/sleep") {
    new AsyncResult() {
      override val is: Future[_] = Future {
        val v = count.incrementAndGet()
        println(s"call sleep $v")
        val sleepTimeInMs = params("time").toLong
        blocking {
          Thread.sleep(sleepTimeInMs)
        }
        "OK"
      }
    }
  }
}
