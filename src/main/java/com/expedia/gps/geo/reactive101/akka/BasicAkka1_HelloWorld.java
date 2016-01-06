package com.expedia.gps.geo.reactive101.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import lombok.Value;

/**
 * A basic actor developed in Java.
 */
public class BasicAkka1_HelloWorld {

  private static ActorSystem system = ActorSystem.create("AKKA_test_in_Java");

  public static void main(String[] args) {
    ActorRef myActor = system.actorOf(Props.create(MyActor.class));
    myActor.tell(new Hello("Brian"), null);

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // nothing null
    }
    system.shutdown();
  }


  @Value
  private static class Hello {
    private final String name;
  }

  private static class MyActor extends UntypedActor {

    @Override
    public void onReceive(Object message) throws Exception {
      if (message instanceof Hello) {
        Hello hello = (Hello)message;
        System.out.println("Hello, " + hello.getName() + "!");
      } else
        unhandled(message);
    }
  }
}
