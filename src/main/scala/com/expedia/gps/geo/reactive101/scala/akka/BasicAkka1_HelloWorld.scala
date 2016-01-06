package com.expedia.gps.geo.reactive101.scala.akka

import akka.actor.{Actor, ActorSystem, Props}

import scala.concurrent.ExecutionContext

/**
  * A basic actor developed in scala.
  */
object BasicAkka1_HelloWorld  {

  val system = ActorSystem("AKKA_test")
  implicit val executionContext: ExecutionContext = system.dispatcher

  def main(args: Array[String]) {
    val myActor = system.actorOf(Props[MyActor])
    myActor ! Hello("Brian")
    system.shutdown()
  }

  case class Hello(name: String)

  class MyActor() extends Actor {
    def receive = {
      case Hello(name) => println(s"Hello $name !")
    }
  }

}
