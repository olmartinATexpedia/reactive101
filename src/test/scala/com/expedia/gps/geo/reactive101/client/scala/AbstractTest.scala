package com.expedia.gps.geo.reactive101.client.scala

import java.util.concurrent.TimeUnit

import com.codahale.metrics.{ConsoleReporter, MetricRegistry, Timer}
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory

/**
  * TODO. 
  */
trait AbstractTest {

  val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
  rootLogger.setLevel(ch.qos.logback.classic.Level.OFF)

  protected implicit val jsonFormats: Formats = DefaultFormats

  private val NB_CALLS: Int = 1000

  val metrics = new MetricRegistry()
  val reporter = ConsoleReporter.forRegistry(metrics)
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .build()
  val mainTimer: Timer = metrics.timer(s"Multiple call")
  val subTimer: Timer = metrics.timer(s"Multiple call.sub")

}
