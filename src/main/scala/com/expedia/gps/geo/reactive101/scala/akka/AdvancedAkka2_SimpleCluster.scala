package com.expedia.gps.geo.reactive101.scala.akka

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.typesafe.config.ConfigFactory

/**
  * TODO. 
  */
object AdvancedAkka2_SimpleCluster {

  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq("2551", "2552"))
    else
      startup(args)
  }

  def startup(ports: Seq[String]): Unit = {
    ports foreach { port =>
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load().getConfig("cluster-example"))
      val system = ActorSystem("ClusterSystem", config)
      system.actorOf(Props[SimpleClusterListener], name = "clusterListener")
    }
  }

  class SimpleClusterListener extends Actor with ActorLogging {

    val cluster = Cluster(context.system)

    // subscribe to cluster changes, re-subscribe when restart
    override def preStart(): Unit = {
      //#subscribe
      cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
        classOf[MemberEvent], classOf[UnreachableMember])
      //#subscribe
    }

    override def postStop(): Unit = cluster.unsubscribe(self)

    def receive = {
      case MemberUp(member) =>
        log.info(s"Member is ${Console.GREEN}Up${Console.RESET}: {}", member.address)
      case UnreachableMember(member) =>
        log.info(s"Member detected as ${Console.YELLOW}unreachable${Console.RESET}: {}", member)
      case MemberRemoved(member, previousStatus) =>
        log.info(s"Member is ${Console.RED}Removed${Console.RESET}: {} after {}",
          member.address, previousStatus)
      case _: MemberEvent => // ignore
    }
  }


}
