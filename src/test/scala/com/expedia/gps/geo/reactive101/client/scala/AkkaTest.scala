package com.expedia.gps.geo.reactive101.client.scala

import akka.actor.{Props, Actor, UntypedActor, ActorSystem}
import com.expedia.gps.geo.reactive101.scala.client.{DispatchRESTClient, CallSuccess, ScalaRESTClient}
import org.json4s.jackson.JsonMethods._

import scala.concurrent.ExecutionContext

/**
  * TODO. 
  */
object AkkaTest extends AbstractTest {

  val system = ActorSystem.create("AKKA test")
  implicit val executionContext: ExecutionContext = system.dispatcher

  def main(args: Array[String]) {
    val NB_ORDERS = 1000
    val waiter = system.actorOf(Waiter.props(new DispatchRESTClient()))
    for (i <- 0 until NB_ORDERS)
      waiter ! TakeOrder
  }

  object Waiter {
    def props(restClient: ScalaRESTClient) = Props(new Client(restClient))
  }
  class Waiter(restClient: ScalaRESTClient) extends Actor {
    var nbOrders = 0
    var foodPrepared: List[String] = Nil
    def receive = {
      case TakeOrder() =>
        val client = context.actorOf(Client.props(restClient))
        client ! TakeOrder()
      case Order(food) =>
        val chef = context.actorOf(Chef.props(restClient))
        chef ! Order(food)
      case FoodReady(food) =>
        foodPrepared = food :: foodPrepared
        nbOrders = nbOrders - 1
        checkFinish()
    }

    def checkFinish() {
      if (nbOrders == 0) {
        println(s"Nb food prepared: ${foodPrepared.size}")
        context.system.shutdown()
      }
    }
  }

  object Client {
    def props(restClient: ScalaRESTClient) = Props(new Client(restClient))
  }
  class Client(restClient: ScalaRESTClient) extends Actor {
    def receive = {
      case TakeOrder() =>
        val sender = context.sender()
        restClient.callAsync("localhost:4200", s"/food/takeOrder")
          .collect({ case success: CallSuccess => success })
          .foreach { r =>
            val food = (parse(r.content) \ "food").extract[String]
            sender ! Order(food)
          }
    }
  }

  object Chef {
    def props(restClient: ScalaRESTClient) = Props(new Chef(restClient))
  }
  class Chef(restClient: ScalaRESTClient) extends Actor {
    def receive = {
      case Order(food) =>
        val sender = context.sender()
        val foodToPrepare = food.charAt(0).toUpper + food.substring(1)
        restClient.callAsync("localhost:4200", s"/food/prepare$foodToPrepare")
          .collect({ case success: CallSuccess => success })
          .foreach { r =>
            val foodPrepared = (parse(r.content) \ "food").extract[String]
            sender ! FoodReady(foodPrepared)
          }
    }
  }

  case class TakeOrder()
  case class Order(food: String)
  case class FoodReady(food: String)
}
