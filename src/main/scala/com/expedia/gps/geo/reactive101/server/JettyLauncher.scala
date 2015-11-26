package com.expedia.gps.geo.reactive101.server

import com.typesafe.scalalogging.StrictLogging
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.slf4j.LoggerFactory


/**
 * Launch a Jetty server.
 * @author olmartin@expedia.com
 * @since 2015-09-22
 */
object JettyLauncher extends App with StrictLogging {
  val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
  rootLogger.setLevel(ch.qos.logback.classic.Level.WARN)

  val port = 4200
  try {
    val server = new Server(port)
    val context = new WebAppContext()
    context setContextPath("/")
    context.setResourceBase("src/main/webapp")
    context.setInitParameter(ScalatraListener.LifeCycleKey, "com.expedia.gps.geo.reactive101.server.ScalatraBootstrap")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    server.setHandler(context)

    server.start()
    server.join()
    println(s"${Console.GREEN_B}Server started on port $port${Console.RESET}")
  } catch {
    case e: Throwable => logger.error(s"Failed to start server om port $port", e)
  }
}
