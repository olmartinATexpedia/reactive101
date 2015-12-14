package com.expedia.gps.geo.reactive101.akka

import akka.actor._

import scala.concurrent.ExecutionContext

/**
  * TODO. 
  */
object BasicAkka3_AboutChildren  {

  val system = ActorSystem.create("AKKA_test")
  implicit val executionContext: ExecutionContext = system.dispatcher

  def main(args: Array[String]) {
    val myActor = system.actorOf(Props(classOf[MyActor], 0), "myActor0")
    val myActorb = system.actorOf(Props(classOf[MyActor], 0), "myActor0b")
    println(myActor)
    myActor ! CreateChildren(3)
    Thread.sleep(100)

//    myActor ! PoisonPill
//    system.actorSelection("/user/myActor0/myActor1/myActor2") ! PoisonPill
//    myActor ! StopChildren(2)

//    system.actorSelection("**/myActor*") ! Identify()
    system.actorSelection("*/myActor*") ! Identify()

    Thread.sleep(100)
    system.shutdown()
  }

  case class CreateChildren(nbChildren: Integer)
  case class StopChildren(depth: Integer)
  case class Identify()

  class MyActor(depth: Integer) extends Actor {
    def receive = {

      case CreateChildren(nbChildren) if nbChildren > 0 =>
        val child = context.actorOf(Props(classOf[MyActor], depth + 1), s"myActor${depth + 1}")
        println(child)
        child ! CreateChildren(nbChildren - 1)

      case stop@StopChildren(childDepth) =>
        val child = context.child(s"myActor${depth + 1}").get
        if (childDepth == depth + 1)
          context.stop(child)
        else
          child ! stop

      case Identify() => println(s"I am ${context.self.path}")
    }

    override def postStop(){
      println(s"${this.context.self.path} is stopped!")
      if (depth == 0)
        system.shutdown()
    }
  }

}
