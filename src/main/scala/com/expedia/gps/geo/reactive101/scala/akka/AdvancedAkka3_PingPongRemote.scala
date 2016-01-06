package com.expedia.gps.geo.reactive101.scala.akka

import akka.actor._
import akka.cluster.{Member, Cluster}
import akka.cluster.ClusterEvent._
import com.typesafe.config.ConfigFactory
import scala.collection.immutable.ListSet
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

/**
  * TODO. 
  */
object AdvancedAkka3_PingPongRemote {

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      val system = startup("2551", "nodes")
      system.actorOf(Props[SimpleClusterListener], name = "clusterListener")
    }
    else {
      val system = startup(args.head, "player")
      system.actorOf(Props[Player], "player")
    }
  }

  def startup(port: String, role: String): ActorSystem = {
    val defaultConfig = ConfigFactory.load()
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
      .withFallback(ConfigFactory.parseString(s"akka.cluster.roles = [$role]"))
      .withFallback(defaultConfig.getConfig("cluster-example").withFallback(defaultConfig))
    ActorSystem("ClusterSystem", config)
  }

  case class Ball(index: Int, count: Int)
  case class PlayerRegistration()

  class Player extends Actor {
    var otherPlayers = ListSet[ActorPath]()
    var ballsSent = Map[ActorPath, Ball]()
    var ballsInHand = ListSet[Ball]()

    val cluster = Cluster(context.system)

    override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp], classOf[UnreachableMember])

    override def postStop(): Unit = cluster.unsubscribe(self)

    def receive = {

      case MemberUp(member) if member.hasRole("player") & member.address != cluster.selfAddress => register(member)

      case PlayerRegistration() => otherPlayers += sender().path

      case ball@Ball(index, count) =>
        selectAPlayer() match {
          case Some(player) =>
            println(s"${Console.BLUE}Receive Ball($index, $count). Send Ball($index, ${count + 1}) to $player${Console.RESET}")
            implicit val ec: ExecutionContext = context.system.dispatcher
            context.system.scheduler.scheduleOnce(500 milliseconds) {
              ballsSent += (player -> ball)
              context.actorSelection(player) ! Ball(index, count + 1)
            }
          case None =>
            ballsInHand += ball
            println(s"${Console.YELLOW}${self.path} waiting for player to play with $ballsInHand${Console.RESET}")
            context.become(waitingForPlayer)
        }


      case UnreachableMember(member) if member.hasRole("player") =>
        val playerPath = findPlayerPath(member)
        otherPlayers = otherPlayers.filter(_ != playerPath)
        if (otherPlayers.isEmpty) {
          ballsInHand ++= ballsSent.values
          println(s"${Console.YELLOW}${self.path} waiting for player to play with $ballsInHand${Console.RESET}")
          context.become(waitingForPlayer)
        } else {
          ballsSent.get(playerPath) foreach { ball =>
            self ! ball
          }
        }
    }

    def waitingForPlayer: Receive = {

      case ball@Ball(index, count) =>
        ballsInHand += ball

      case MemberUp(member) if member.hasRole("player") & member.address != cluster.selfAddress =>
        println(s"${Console.GREEN}${self.path} found a player to play with at address ${member.address}!${Console.RESET}")
        register(member)
        context.become(receive)
        val allBalls = ListSet[Ball]() ++ ballsInHand
        ballsInHand = ListSet[Ball]()
        allBalls foreach { ball =>
          self ! ball
        }

    }

    def selectAPlayer(): Option[ActorPath] = Random.shuffle(otherPlayers).headOption

    def register(member: Member) {
      val path = findPlayerPath(member)
      if (!otherPlayers.contains(path)) {
        otherPlayers += path
        context.actorSelection(path) ! PlayerRegistration()
      }
    }

    def findPlayerPath(member: Member): ActorPath = RootActorPath(member.address) / "user" / "player"
  }

  class SimpleClusterListener extends Actor with ActorLogging {

    val cluster = Cluster(context.system)
    var players = ListSet[Address]()
    var count = 0

    override def preStart(): Unit = {
      cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
        classOf[MemberEvent], classOf[UnreachableMember])
    }

    override def postStop(): Unit = cluster.unsubscribe(self)

    def receive = {
      case MemberUp(member) if member.hasRole("player") =>
        println(s"${Console.GREEN}Found a new player ${member.address} !${Console.RESET}")
        if (players.isEmpty) {
          count = count + 1
          val actorPath = RootActorPath(member.address) / "user" / "player"
          println(s"${Console.BLUE}Send a ball to $actorPath${Console.RESET}")
          context.actorSelection(actorPath) ! Ball(count, 0)
        }
        players += member.address

      case UnreachableMember(member) if member.hasRole("player") =>
        println(s"${Console.YELLOW}Player ${member.address} unreachable...${Console.RESET}")
        players = players.filter(_ != member.address)

      case MemberRemoved(member, previousStatus) =>
        println(s"${Console.RED}Player ${member.address} removed${Console.RESET}")
      case _: MemberEvent => // ignore
    }
  }


}
