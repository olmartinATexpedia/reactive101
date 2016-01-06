package com.expedia.gps.geo.reactive101.akka;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import lombok.Value;

/**
 * A conversation between two actors.
 */
public class BasicAkka2_Conversation {

  private static ActorSystem system = ActorSystem.create("AKKA_test_in_Java");

  public static void main(String[] args) {
    ActorRef paul = system.actorOf(Props.create(Paul.class), "paul");
    ActorRef jacques = system.actorOf(Props.create(Jacques.class), "jacques");
    paul.tell(new StartConversation(jacques.path()), null);

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // nothing null
    }
    system.shutdown();
  }

  private static class Paul extends UntypedActor {

    @Override
    public void onReceive(Object message) throws Exception {
      if (message instanceof StartConversation) {
        StartConversation startConversation = (StartConversation)message;
        context().actorSelection(startConversation.getWho()).tell(new Question("Hello " + startConversation.getWho().name()), getSelf());
      } else if (message instanceof Answer) {
        Answer answer = (Answer) message;
        if (answer.getValue().contains("Hello")) {
          System.out.println(answer.getValue());
          context().sender().tell(new Question("How are you?"), getSelf());
        } else
          System.out.println(answer.getValue());
      } else
        unhandled(message);
    }
  }

  private static class Jacques extends UntypedActor {

    @Override
    public void onReceive(Object message) throws Exception {
      if (message instanceof Question) {
        Question question = (Question) message;
        if (question.getValue().contains("Hello")) {
          System.out.println(question.getValue());
          context().sender().tell(new Answer("Hello " + context().sender() + "!"), getSelf());
        } else if (question.getValue().contains("How are you")) {
          System.out.println(question.getValue());
          context().sender().tell(new Answer("I am fine thanks"), getSelf());
        } else
          unhandled(message);
      } else
        unhandled(message);
    }
  }


  @Value
  private static class StartConversation {
      private ActorPath who;
  }

  @Value
  private static class Question {
    private String value;
  }

  @Value
  private static class Answer {
    private String value;
  }
}
