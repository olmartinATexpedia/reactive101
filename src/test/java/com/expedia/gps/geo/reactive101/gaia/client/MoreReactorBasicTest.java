package com.expedia.gps.geo.reactive101.gaia.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import reactor.Environment;
import reactor.fn.Function;
import reactor.rx.Promise;
import reactor.rx.broadcast.Broadcaster;

import com.codahale.metrics.Timer;
import com.expedia.gps.geo.reactive101.gaia.client.type.CallSuccess;
import com.expedia.gps.geo.reactive101.gaia.client.type.SimpleResponse;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author olmartin@expedia.com
 * @since 2015-11-16
 */
@Slf4j
public class MoreReactorBasicTest extends MultithreadedJavaTest{
  private static final int NB_CALLS = 100;
  static {
    Environment.initialize();
  }

  public static void main(String[] args) throws Exception {
    ReactorHttpClient client = new ReactorHttpClient();
    run(client.getClass().getSimpleName(), client);
  }

  private static void run(String id, ReactorHttpClient client) throws Exception {
    Timer main = metrics.timer("Multiple call " + id);
    Timer.Context mainContext = main.time();
    List<String> foods = new ArrayList<>();
    AtomicInteger errorCount = new AtomicInteger(0);
    Broadcaster<Integer> broadcaster = Broadcaster.create();
    broadcaster
        .flatMap(index -> askOrder(client))
          .flatMap(food -> prepareFood(client, food))
          .buffer(5)
          .observe(foods::addAll)
          .consume();
    for (int i=0; i< NB_CALLS; i++) {
      broadcaster.onNext(i);
    }
    while (foods.size() + errorCount.get() < NB_CALLS) {
      Thread.sleep(2000);
      System.out.print("\rNb foods prepared: " + foods.size());
    }
    System.out.println();
    mainContext.close();
    reporter.report();
    Environment.terminate();
    System.exit(0);
  }


  private static Promise<String> askOrder(ReactorHttpClient client) {
    try {
      return client.callAsync("localhost:4200", "/food/takeOrder")
          .map(analyzeOrder);
    } catch (Exception e) {
      log.error("An error occurs", e);
      throw new IllegalStateException("Failed to get order");
    }
  }

  private static Function<SimpleResponse, String> analyzeOrder = simpleResponse -> {
    CallSuccess success = (CallSuccess) simpleResponse;
    JsonNode actualObj = null;
    try {
      actualObj = mapper.readTree(success.getContent());
      String food = actualObj.get("order").asText();
      //      System.out.println("order is " + food);
      return food.substring(0, 1).toUpperCase() + food.substring(1);
    } catch (IOException e) {
      throw new IllegalStateException("Take order failed");
    }
  };

    private static Promise<String> prepareFood(ReactorHttpClient client, String food) {
    try {
            return client.callAsync("localhost:4200", "/food/prepare" + food)
                .map(checkFoodIsReady);
    } catch (Exception e) {
      log.error("An error occurs", e);
      throw new IllegalStateException("Failed to get order");
    }
  }

  private static final Function<SimpleResponse, String> checkFoodIsReady = simpleResponse -> {
    CallSuccess success = (CallSuccess) simpleResponse;
    JsonNode actualObj = null;
    try {
      actualObj = mapper.readTree(success.getContent());
      String foodPrepared = actualObj.get("food").asText();
//      System.out.println(foodPrepared);
      return foodPrepared;
    } catch (IOException e) {
      throw new IllegalStateException("Food preparation failed");
    }
  };
}
