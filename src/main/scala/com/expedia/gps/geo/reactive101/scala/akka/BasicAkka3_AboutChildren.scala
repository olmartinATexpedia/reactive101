package com.expedia.gps.geo.reactive101.akka

import akka.actor._

import scala.concurrent.ExecutionContext

/**
  * Creation of children, kill children.
  */
object BasicAkka3_AboutChildren  {

  val system = ActorSystem("AKKA_test")
  implicit val executionContext: ExecutionContext = system.dispatcher

  def main(args: Array[String]) {
    val myActor = system.actorOf(Props(classOf[MyActor], 0), "myActor")
    val myActorb = system.actorOf(Props(classOf[MyActor], 0), "myActor2")
    println(myActor)
    myActor ! CreateChildren(3)
    Thread.sleep(100)
    system.actorSelection("*/myActor*") ! Identify(deep = true)

//    system.actorSelection("/user/myActor0/myActor1/myActor2") ! PoisonPill
//    myActor ! StopChildren(2)

//    myActor ! PoisonPill

    Thread.sleep(100)
    system.shutdown()
  }

  case class CreateChildren(nbChildren: Integer)
  case class StopChildren(depth: Integer)
  case class Identify(deep: Boolean = false)

  class MyActor(depth: Integer) extends Actor {
    def receive = {

      case CreateChildren(nbChildren) if nbChildren > 0 =>
        val child = context.actorOf(Props(classOf[MyActor], depth + 1), s"myActorChild${depth + 1}")
        println(child)
        child ! CreateChildren(nbChildren - 1)

      case stop@StopChildren(childDepth) =>
        val child = context.child(s"myActor${depth + 1}").get
        if (childDepth == depth + 1)
          context.stop(child)
        else
          child ! stop

      case identify@Identify(deep) =>
        println(s"I am ${context.self.path}")
        if (deep)
          context.children.foreach(_ ! identify)
    }

    override def postStop(){
      println(s"${this.context.self.path} is stopped!")
      if (depth == 0)
        system.shutdown()
    }
  }

}
