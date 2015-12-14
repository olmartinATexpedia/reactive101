package com.expedia.gps.geo.reactive101.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import com.expedia.gps.geo.reactive101.client.type.CallFailure;
import com.expedia.gps.geo.reactive101.client.type.CallSuccess;
import com.expedia.gps.geo.reactive101.client.type.SimpleResponse;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

/**
 * An async HTTP client.
 *
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
public class AsyncRESTClient implements RestClient {

  private AsyncHttpClient asyncHttpClient = new AsyncHttpClient(
      new AsyncHttpClientConfig.Builder()
          .setMaxConnections(100)
          .setMaxConnectionsPerHost(100)
          .setAllowPoolingConnections(true)
          .build());

  @Override
  public SimpleResponse call(String host, String url) throws Exception {
    return callAsync(host, url).get();
  }

  @Override
  public Future<SimpleResponse> callAsync(String host, String url) throws Exception {
    String finalURL = "http://" + host + url;
    ListenableFuture<SimpleResponse> f = asyncHttpClient.prepareGet(finalURL).execute(
        new AsyncCompletionHandler<SimpleResponse>() {
          @Override
          public SimpleResponse onCompleted(Response response) throws Exception {
            int statusCode = response.getStatusCode();
            String content = response.getResponseBody();
            if (statusCode == 200) {
              return new CallSuccess(content);
            } else {
              return new CallFailure(statusCode, content);
            }
          }
        });
    return f;
  }

  @Override
  public CompletableFuture<SimpleResponse> callAsync2(String host, String url, Executor executor) throws Exception {
    CompletableFuture<SimpleResponse> completableFuture = new CompletableFuture<>();
    ListenableFuture<SimpleResponse> simpleResponseFuture = (ListenableFuture<SimpleResponse>)callAsync(host, url);
    simpleResponseFuture.addListener((Runnable) () -> {
      SimpleResponse simpleResponse = null;
      try {
        simpleResponse = simpleResponseFuture.get();
        completableFuture.complete(simpleResponse);
      } catch (Exception e) {
        completableFuture.completeExceptionally(e);
      }
    }, executor);
    return completableFuture;
  }
}
