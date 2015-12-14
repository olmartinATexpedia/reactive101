package com.expedia.gps.geo.reactive101.client;

import java.util.concurrent.TimeUnit;

import lombok.Getter;

import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO.
 */
@Getter
public abstract class AbstractTest {

  private static final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory
      .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
  static {
    rootLogger.setLevel(ch.qos.logback.classic.Level.OFF);
  }


  private final RestClient   restClient;
  private final Timer main;
  private final Timer.Context mainContext;

  private final ObjectMapper mapper     = new ObjectMapper();

  private final MetricRegistry    metrics  = new MetricRegistry();
  private final ScheduledReporter reporter = ConsoleReporter.forRegistry(metrics)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build();

  public AbstractTest(String mainTimerName, RestClient restClient) {
    this.restClient = restClient;
    this.main = metrics.timer(mainTimerName);
    this.mainContext = main.time();
  }
}
