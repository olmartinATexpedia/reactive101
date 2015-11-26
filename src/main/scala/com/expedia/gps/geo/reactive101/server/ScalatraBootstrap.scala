package com.expedia.gps.geo.reactive101.server


import javax.servlet.ServletContext

import _root_.akka.actor.ActorSystem
import com.expedia.gps.geo.reactive101.server.controller.{FoodController, BasicController}
import com.typesafe.scalalogging.StrictLogging
import org.scalatra._

import scala.language.postfixOps

/**
 *
 * @author olmartin@expedia.com
 * @since 2015-09-21
 */
class ScalatraBootstrap extends LifeCycle with StrictLogging {

  val system = ActorSystem()

  override def init(context: ServletContext) {
    context.mount(new BasicController(system), "/")
    context.mount(new FoodController(system), "/food")
  }

}