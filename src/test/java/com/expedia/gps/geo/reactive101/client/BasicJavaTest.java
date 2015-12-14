package com.expedia.gps.geo.reactive101.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.expedia.gps.geo.reactive101.client.type.CallSuccess;
import com.expedia.gps.geo.reactive101.client.type.CallSuccess;
import com.expedia.gps.geo.reactive101.client.type.SimpleResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author olmartin@expedia.com
 * @since 2015-11-16
 */
public class BasicJavaTest {

  private static final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory
      .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

  static {
    rootLogger.setLevel(ch.qos.logback.classic.Level.OFF);
  }

  private static final int             NB_CALLS = 10;
  private static final MetricRegistry  metrics  = new MetricRegistry();
  private static final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build();
  private static final ObjectMapper    mapper   = new ObjectMapper();

  private interface ClientMethodCall {
    SimpleResponse call(String host, String path) throws Exception;
  }

  private interface MainLogic {
    String execute(ClientMethodCall methodCall) throws Exception;
  }

  private static MainLogic foodOrder = (m) -> {
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

  public static void main(String[] args) throws Exception {
    RestClient client = new BasicRESTClient();
    //    RestClient client = new AsyncRESTClient();

    Timer main = metrics.timer("Multiple call " + client.getClass().getSimpleName());
    Timer sub = metrics.timer("Multiple call.sub " + client.getClass().getSimpleName());
    Timer.Context mainContext = main.time();
    List<String> responses = new ArrayList<>();
    for (int i = 0; i < NB_CALLS; i++) {
      Timer.Context subContext = sub.time();

      SimpleResponse orderReturned = client.call("localhost:4200", "/food/takeOrder");
      if (orderReturned instanceof CallSuccess) {
        JsonNode actualObj = mapper.readTree(((CallSuccess) orderReturned).getContent());
        String food = actualObj.get("order").asText();
        SimpleResponse foodPrepared = client.call("localhost:4200", "/food/prepare" + food.substring(0, 1).toUpperCase() + food.substring(1));
        if (foodPrepared instanceof CallSuccess) {
          actualObj = mapper.readTree(((CallSuccess) foodPrepared).getContent());
          responses.add(actualObj.get("food").asText());
        } else {
          throw new IllegalStateException("Food preparation failed");
        }
      } else {
        throw new IllegalStateException("Take order failed");
      }

      subContext.close();
    }
    mainContext.close();
    reporter.report();
    System.exit(0);

  }
}
