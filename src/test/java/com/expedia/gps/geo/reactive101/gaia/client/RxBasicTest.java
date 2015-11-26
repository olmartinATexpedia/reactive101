package com.expedia.gps.geo.reactive101.gaia.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.functions.Func1;

import com.codahale.metrics.Timer;
import com.expedia.gps.geo.reactive101.gaia.client.type.CallSuccess;
import com.expedia.gps.geo.reactive101.gaia.client.type.SimpleResponse;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author olmartin@expedia.com
 * @since 2015-11-16
 */
@Slf4j
public class RxBasicTest extends MultithreadedJavaTest{
  private static final int             NB_CALLS   = 1000;

  private interface ClientMethodCall {
    Observable<SimpleResponse> call(String host, String path) throws Exception;
  }

  private interface MainLogic {
    Observable<String> execute(ClientMethodCall methodCall) throws Exception;
  }

  private static final Func1<SimpleResponse, String> analyzeOrder = simpleResponse -> {
    CallSuccess success = (CallSuccess) simpleResponse;
    JsonNode actualObj = null;
    try {
      actualObj = mapper.readTree(success.getContent());
      String food = actualObj.get("order").asText();
      System.out.println("order is " + food);
      return food.substring(0, 1).toUpperCase() + food.substring(1);
    } catch (IOException e) {
      throw new IllegalStateException("Take order failed");
    }
  };

  private static final Func1<SimpleResponse, String> analyseFoodReturned = simpleResponse -> {
    CallSuccess success = (CallSuccess) simpleResponse;
    JsonNode actualObj = null;
    try {
      actualObj = mapper.readTree(success.getContent());
      return actualObj.get("food").asText();
    } catch (IOException e) {
      throw new IllegalStateException("Food preparation failed");
    }
  };

  private static MainLogic foodOrder = (m) -> {
    System.out.println("Ask order");
    return m.call("localhost:4200", "/food/takeOrder")
        .take(1)
        .map(analyzeOrder)
        .map(food -> {
          try {
            System.out.println("ask to prepare " + food);
            return m.call("localhost:4200", "/food/prepare" + food).map(analyseFoodReturned).take(1).toBlocking().first();
          } catch (Exception e) {
            throw new IllegalStateException("Food preparation failed");
          }
        });
  };

  public static void main(String[] args) throws Exception {
    RXAsyncRESTClient client = new RXAsyncRESTClient();
    run(client.getClass().getSimpleName(), foodOrder, client::callAsync);
  }

  private static void run(String id, MainLogic logic, ClientMethodCall clientMethodCall) throws Exception {
    Timer main = metrics.timer("Multiple call " + id);
    //    Timer sub = metrics.timer("Multiple call.sub " + id);
    Timer.Context mainContext = main.time();
    List<String> foods = new ArrayList<>();
    AtomicInteger errorCount = new AtomicInteger(0);
    for (int i = 0; i < NB_CALLS; i++) {
      logic.execute(clientMethodCall).take(1).doOnNext(food -> {foods.add(food);System.out.println("Finish ");});
    }
    while (foods.size() + errorCount.get() < NB_CALLS) {
      Thread.sleep(50);
    }
    System.out.println("Nb foods prepared: " + foods.size());
    mainContext.close();
    reporter.report();
    System.exit(0);
  }
}
