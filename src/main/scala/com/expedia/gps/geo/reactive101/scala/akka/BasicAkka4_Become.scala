package com.expedia.gps.geo.reactive101.scala.akka

import akka.actor.{Actor, ActorPath, ActorSystem, Props}

import scala.concurrent.ExecutionContext

/**
  * TODO. 
  */
object BasicAkka4_Become  {

  val system = ActorSystem.create("AKKA_test")
  implicit val executionContext: ExecutionContext = system.dispatcher

  def main(args: Array[String]) {
    val paul = system.actorOf(Props[Paul], "paul")
    val jacques = system.actorOf(Props[Jacques], "jacques")
    paul ! StartConversation(jacques.path)

    Thread.sleep(100)
    system.shutdown()
  }

  class Paul() extends Actor {
    import context._
    def receive: Receive = {
      case StartConversation(who) =>
        val person = context.actorSelection(who)
        person ! Question(s"Hello $who !")
        become(waitingForHello)
    }

    def waitingForHello: Receive  = {
      case Answer(msg) =>
        println(msg)
        sender() ! Question("How are you?")
        become(waitingForHowAreYouAnswer)
    }

    def waitingForHowAreYouAnswer: Receive  = {
      case Answer(msg) =>
        println(msg)
        become(receive)
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
  case class Answer(name: String)
}
