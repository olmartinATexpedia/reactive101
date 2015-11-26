package com.expedia.gps.geo.reactive101.gaia.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.expedia.gps.geo.reactive101.gaia.client.type.CallSuccess;
import com.expedia.gps.geo.reactive101.gaia.client.type.SimpleResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author olmartin@expedia.com
 * @since 2015-11-16
 */
public class CompletableFutureJavaTest {

  private static final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory
      .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

  static {
    rootLogger.setLevel(ch.qos.logback.classic.Level.OFF);
  }

  private static final int             NB_CALLS = 100;
  private static final MetricRegistry  metrics  = new MetricRegistry();
  private static final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build();
  private static final ObjectMapper    mapper   = new ObjectMapper();

  private interface ClientMethodCall {
    CompletableFuture<SimpleResponse> call(String host, String path) throws Exception;
  }

  private interface MainLogic {
    void execute(ClientMethodCall methodCall, List<String> foods, AtomicInteger errors) throws Exception;
  }

  private static MainLogic foodOrder = (m, foods, errors) -> {
    try {
      CompletableFuture<SimpleResponse> orderReturned = m.call("localhost:4200", "/food/takeOrder");
      orderReturned.thenAccept(simpleResponse -> {
        if (simpleResponse instanceof CallSuccess) {
          try {
            JsonNode actualObj = mapper.readTree(((CallSuccess) simpleResponse).getContent());
            String food = actualObj.get("order").asText();
            CompletableFuture<SimpleResponse> foodPreparedFuture = m
                .call("localhost:4200", "/food/prepare" + food.substring(0, 1).toUpperCase() + food.substring(1));
            foodPreparedFuture.thenAccept(foodPrepared -> {
              if (foodPrepared instanceof CallSuccess) {
                try {
                  JsonNode obj = mapper.readTree(((CallSuccess) foodPrepared).getContent());
                  foods.add(obj.get("food").asText());
                } catch (Exception e) {
                  errors.incrementAndGet();
                  throw new IllegalStateException("Take order failed");
                }
              } else {
                errors.incrementAndGet();
                throw new IllegalStateException("Food preparation failed");
              }
            });
          } catch (Exception e) {
            errors.incrementAndGet();
            throw new IllegalStateException("Take order failed");
          }
        } else {
          errors.incrementAndGet();
          throw new IllegalStateException("Take order failed");
        }
      });
    } catch (Exception e) {
      errors.incrementAndGet();
    }
  };

  public static void main(String[] args) throws Exception {
    BasicRESTClient client = new BasicRESTClient();
    run(client.getClass().getSimpleName(), foodOrder, client::callCompletableFuture);
  }

  private static void run(String id, MainLogic logic, ClientMethodCall clientMethodCall) throws Exception {
    Timer main = metrics.timer("Multiple call " + id);
    Timer.Context mainContext = main.time();
    List<String> foods = new ArrayList<>();
    AtomicInteger errors = new AtomicInteger(0);
    for (int i = 0; i < NB_CALLS; i++) {
      logic.execute(clientMethodCall, foods, errors);
    }
    while (foods.size() + errors.get() < NB_CALLS) {
      Thread.sleep(200);
      System.out.print("\rNb foods prepared: " + foods.size());
    }
    System.out.println("\rNb foods prepared: " + foods.size());
    mainContext.close();
    reporter.report();
    System.exit(0);
  }
}
