package com.expedia.gps.geo.reactive101.gaia.client.scala

import java.util.concurrent.TimeUnit

import com.codahale.metrics.Timer.Context
import com.codahale.metrics._
import com.expedia.gps.geo.reactive101.scala.client._
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
 *
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
object BasicScalaTest {

  val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
  rootLogger.setLevel(ch.qos.logback.classic.Level.OFF)

  import scala.concurrent.ExecutionContext.Implicits.global
  protected implicit val jsonFormats: Formats = DefaultFormats

  private val NB_CALLS: Int = 1000

  val metrics = new MetricRegistry()
  val reporter = ConsoleReporter.forRegistry(metrics)
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .build()
  val client = new DispatchRESTClient
  val main: Timer = metrics.timer(s"Multiple call ${client.getClass.getSimpleName}")
  val sub: Timer = metrics.timer(s"Multiple call.sub ${client.getClass.getSimpleName}")

  def main(args: Array[String]) {
    val mainContext: Timer.Context = main.time()
    val futures: Future[Seq[String]] = doMultipleCall(client, metrics)
    futures onComplete { foods =>
      println(s"Nb food prepared: ${foods.get.size}")
      mainContext.close()
      reporter.report()
      sys.exit(0)
    }
  }

  def orderAndGetFood: Future[String] = {
    val subContext: Context = sub.time()
    client
      .callAsync("localhost:4200", "/food/takeOrder")
      .collect({ case success: CallSuccess => success })
      .map { success =>
        val food = (parse(success.content) \ "order").extract[String]
        food.charAt(0).toUpper + food.substring(1)
      }
      .flatMap {
        food => client.callAsync("localhost:4200", s"/food/prepare$food")
      }
      .collect({ case success: CallSuccess => success })
      .map { success =>
        val foodPrepared = (parse(success.content) \ "food").extract[String]
        subContext.close()
        foodPrepared
      }
  }

  def doMultipleCall(client: ScalaRESTClient, metrics: MetricRegistry): Future[Seq[String]] = {
    val allCallsFutures = (1 to NB_CALLS) map { _ =>
      orderAndGetFood
    }
    Future.sequence(allCallsFutures)
  }

}
