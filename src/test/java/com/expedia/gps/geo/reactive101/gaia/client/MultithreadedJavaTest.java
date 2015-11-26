package com.expedia.gps.geo.reactive101.gaia.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
public class MultithreadedJavaTest {

  private static final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory
      .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

  static {
    rootLogger.setLevel(ch.qos.logback.classic.Level.OFF);
  }

  private static final int             NB_THREADS = 100;
  private static final int             NB_CALLS   = 1000;
  protected static final MetricRegistry  metrics    = new MetricRegistry();
  protected static final ConsoleReporter reporter   = ConsoleReporter.forRegistry(metrics)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build();
  protected static final ObjectMapper    mapper     = new ObjectMapper();

  private interface ClientMethodCall {
    SimpleResponse call(String host, String path) throws Exception;
  }

  private interface MainLogic {
    String execute(ClientMethodCall methodCall) throws Exception;
  }

  protected static MainLogic foodOrder = (m) -> {
    SimpleResponse orderReturned = m.call("localhost:4200", "/food/takeOrder");
    if (orderReturned instanceof CallSuccess) {
      JsonNode actualObj = mapper.readTree(((CallSuccess) orderReturned).getContent());
      String food = actualObj.get("order").asText();
      SimpleResponse foodPrepared = m.call("localhost:4200", "/food/prepare" + food.substring(0, 1).toUpperCase() + food.substring(1));
      if (foodPrepared instanceof CallSuccess) {
        actualObj = mapper.readTree(((CallSuccess) foodPrepared).getContent());
        return actualObj.get("food").asText();
      } else {
        throw new IllegalStateException("Food preparation failed");
      }
    } else {
      throw new IllegalStateException("Take order failed");
    }
  };

  private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(NB_THREADS, NB_THREADS, 1000, TimeUnit.MILLISECONDS,
      new ArrayBlockingQueue<>(NB_CALLS));

  public static void main(String[] args) throws Exception {
//    RestClient client = new BasicRESTClient();
        RestClient client = new AsyncRESTClient();
    run(client.getClass().getSimpleName(), foodOrder, client::call);
        run(client.getClass().getSimpleName(), foodOrder, (host, port) -> client.callAsync(host, port).get());
  }

  private static void run(String id, MainLogic logic, ClientMethodCall clientMethodCall) throws Exception {
    Timer main = metrics.timer("Multiple call " + id);
    Timer sub = metrics.timer("Multiple call.sub " + id);
    Timer.Context mainContext = main.time();
    List<Future<String>> futures = new ArrayList<>();
    for (int i = 0; i < NB_CALLS; i++) {
      Future<String> future = EXECUTOR.submit(() -> {
        Timer.Context subContext = sub.time();
        try {
          return logic.execute(clientMethodCall);
        } finally {
          subContext.close();
        }
      });
      futures.add(future);
    }
    List<String> foods = new ArrayList<>();
    for (Future<String> future : futures) {
      foods.add(future.get());
    }
    System.out.println("Nb foods prepared: " + foods.size());
    mainContext.close();
    reporter.report();
    System.exit(0);
  }
}
