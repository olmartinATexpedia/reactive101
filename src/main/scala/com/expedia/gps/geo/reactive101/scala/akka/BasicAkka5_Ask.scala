package com.expedia.gps.geo.reactive101.scala.akka

import akka.actor.{Actor, ActorPath, ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import akka.pattern.ask

/**
  * TODO. 
  */
object BasicAkka5_Ask  {

  val system = ActorSystem("AKKA_test")
  implicit val executionContext: ExecutionContext = system.dispatcher

  def main(args: Array[String]) {
    val paul = system.actorOf(Props[Paul], "paul")
    val jacques = system.actorOf(Props[Jacques], "jacques")
    paul ! StartConversation(jacques.path)

    Thread.sleep(100)
    system.shutdown()
  }

  class Paul() extends Actor {
    def receive: Receive = {

      case StartConversation(who) =>
        val person = context.actorSelection(who)
        implicit val timeout = Timeout(3 seconds)
        val future: Future[Answer] = (person ? Question(s"Hello $who !")).mapTo[Answer]
        future map { answer =>
          println(answer.message)
          val future: Future[Answer] = (person ? Question("How are you?")).mapTo[Answer]
          future map { answer =>
            println(answer.message)
          }
        }
    }
  }


  class Jacques() extends Actor {
    def receive = {

      case Question(msg) if msg.contains("Hello") =>
        println(msg)
        sender ! Answer(s"Hello ${sender()} !")

      case Question(msg) if msg.contains("How are you") =>
        println(msg)
        sender() ! Answer("I am fine thanks!")

      case Question(msg) =>
        println(msg)
    }
  }

  case class StartConversation(who: ActorPath)
  case class Question(name: String)
  case class Answer(message: String)
}
