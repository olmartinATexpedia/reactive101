package com.expedia.gps.geo.reactive101.gaia.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.expedia.gps.geo.reactive101.gaia.client.type.CallFailure;
import com.expedia.gps.geo.reactive101.gaia.client.type.CallSuccess;
import com.expedia.gps.geo.reactive101.gaia.client.type.SimpleResponse;

/**
 * A basic HTTP client.
 * @author olmartin@expedia.com
 * @since 2015-11-12
 */
@Slf4j
public class BasicRESTClient implements RestClient {

  private CloseableHttpClient httpClient =
      HttpClientBuilder.create()
          .setMaxConnTotal(100)
          .setMaxConnPerRoute(100)
          .build();

  public SimpleResponse call(String host, String url) throws Exception {
    HttpGet httpGet = new HttpGet(url);
    CloseableHttpResponse response = httpClient.execute(HttpHost.create(host), httpGet);
    String encoding = "UTF-8";
    String content = IOUtils.toString(response.getEntity().getContent(), encoding);
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == 200)
      return new CallSuccess(content);
    else
      return new CallFailure(statusCode, content);
  }

  @Override
  public Future<SimpleResponse> callAsync(String host, String url) throws Exception {
    return null;
  }


















  public CompletableFuture<SimpleResponse> callCompletableFuture(String host, String url) throws Exception {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return call(host, url);
      } catch (Exception e) {
        log.error("An error occurs", e);
        return null;
      }
    });
  }
}
