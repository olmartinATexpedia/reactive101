package com.expedia.gps.geo.reactive101.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.expedia.gps.geo.reactive101.client.type.CallSuccess;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author olmartin@expedia.com
 * @since 2015-11-27
 */
public class AkkaTest extends AbstractTest{

  public AkkaTest() {
    super("Akka test", new BasicRESTClient());

    ActorSystem system = ActorSystem.create("AkkaTest");
    ActorRef waiter = system.actorOf(Props.create(Waiter.class, (Creator<Waiter>) () -> new Waiter(getRestClient(), getMainContext(), getReporter(), getMapper())), "waiter");
    int nbOrders = 100;
    for (int i = 0; i < nbOrders; i++) {
      waiter.tell(new ServeClient(), ActorRef.noSender());
    }
  }

  public static void main(String[] args) {
    new AkkaTest();
  }

  private class Waiter extends UntypedActor {

    private final RestClient        restClient;
    private final Timer.Context     mainContext;
    private final ScheduledReporter reporter;
    private final ObjectMapper mapper;

    public Waiter(RestClient restClient, Timer.Context mainContext, ScheduledReporter reporter, ObjectMapper mapper) {
      this.restClient = restClient;
      this.mainContext = mainContext;
      this.reporter = reporter;
      this.mapper = mapper;
    }

    private int          orderPending = 0;
    private List<String> meals        = new ArrayList<>();

    @Override
    public void onReceive(Object message) throws Exception {
      if (message instanceof ServeClient) {
        orderPending++;
        ActorRef client = context().actorOf(Props.create(Client.class, (Creator<Client>) () -> new Client(restClient, mapper)));
        client.tell(new TakeOrder(), getSelf());
      } else if (message instanceof Order) {
        context().stop(getSender());
        ActorRef chef = context().actorOf(Props.create(Chef.class, (Creator<Chef>) () -> new Chef(restClient, mapper)));
        chef.tell(new PreparedFood(((Order) message).getFood()), getSelf());
      } else if (message instanceof FoodReady) {
        context().stop(getSender());
        orderPending--;
        meals.add(((FoodReady)message).getFood());
        checkFinish(context().system());
      } else if (message instanceof OrderFailed) {
        orderPending--;
        checkFinish(context().system());
      } else {
        unhandled(message);
      }
    }

    private void checkFinish(ActorSystem system) {
      if (orderPending <= 0) {
        System.out.println("Nb foods prepared: " + meals.size());
        mainContext.close();
        reporter.report();
        system.shutdown();
        System.exit(0);
      }
    }
  }

  private static class Client extends UntypedActor {

    private final RestClient restClient;
    private final ObjectMapper mapper;

    public Client(RestClient restClient, ObjectMapper mapper) {
      this.restClient = restClient;
      this.mapper = mapper;
    }

    @Override
    public void onReceive(Object message) throws Exception {
      if (message instanceof TakeOrder) {
        ActorRef sender = getSender();
        restClient.callAsync2("localhost:4200", "/food/takeOrder", context().system().dispatcher()).thenAccept(orderReturned -> {
          try {
            if (orderReturned instanceof CallSuccess) {
              JsonNode actualObj = null;
              actualObj = mapper.readTree(((CallSuccess) orderReturned).getContent());
              String food = actualObj.get("order").asText();
              sender.tell(new Order(food), getSelf());
            } else {
              sender.tell(new OrderFailed(), getSelf());
            }
          } catch (IOException e) {
            sender.tell(new OrderFailed(), getSelf());
          }
        });
      } else {
        unhandled(message);
      }
    }
  }

  private static class Chef extends UntypedActor {

    private final RestClient restClient;
    private final ObjectMapper mapper;

    public Chef(RestClient restClient, ObjectMapper mapper) {
      this.restClient = restClient;
      this.mapper = mapper;
    }

    @Override
    public void onReceive(Object message) throws Exception {
      if (message instanceof PreparedFood) {
        ActorRef sender = getSender();
        String food = ((PreparedFood) message).getFood();
        restClient.callAsync2("localhost:4200", "/food/prepare" + food.substring(0, 1).toUpperCase() + food.substring(1), context().system().dispatcher()).thenAccept(foodPreparedResponse -> {
          try {
            if (foodPreparedResponse instanceof CallSuccess) {
              JsonNode actualObj = mapper.readTree(((CallSuccess) foodPreparedResponse).getContent());
              String foodPrepared = actualObj.get("food").asText();
              sender.tell(new FoodReady(foodPrepared), getSelf());
            } else {
              sender.tell(new OrderFailed(), getSelf());
            }
          } catch (IOException e) {
            sender.tell(new OrderFailed(), getSelf());
          }
        });
      } else {
        unhandled(message);
      }
    }
  }

  private static class ServeClient {
  }

  private static class TakeOrder {
  }

  @Getter
  @AllArgsConstructor
  private static class Order {
    private String food;
  }

  @Getter
  @AllArgsConstructor
  private static class PreparedFood {
    private String food;
  }

  @Getter
  @AllArgsConstructor
  private static class FoodReady{
    private String food;
  }
  private static class OrderFailed {}
}
