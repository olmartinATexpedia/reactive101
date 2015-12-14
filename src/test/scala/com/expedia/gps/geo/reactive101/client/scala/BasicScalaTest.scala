package com.expedia.gps.geo.reactive101.client.scala

import com.codahale.metrics.Timer.Context
import com.codahale.metrics._
import com.expedia.gps.geo.reactive101.scala.client._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.Future

/**
 *
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
object BasicScalaTest extends AbstractTest {

  val NB_CALLS = 1000
  import scala.concurrent.ExecutionContext.Implicits.global
  val client = new DispatchRESTClient

  def main(args: Array[String]) {
    val mainContext: Timer.Context = mainTimer.time()
    val futures: Future[Seq[String]] = doMultipleCall(client, metrics)
    futures onComplete { foods =>
      println(s"Nb food prepared: ${foods.get.size}")
      mainContext.close()
      reporter.report()
      sys.exit(0)
    }
  }

  def orderAndGetFood: Future[String] = {
    val subContext: Context = subTimer.time()
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
