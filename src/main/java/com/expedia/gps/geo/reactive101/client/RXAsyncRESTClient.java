package com.expedia.gps.geo.reactive101.client;

import java.util.concurrent.Future;

import rx.Observable;

import com.expedia.gps.geo.reactive101.client.type.CallSuccess;
import com.expedia.gps.geo.reactive101.client.type.CallFailure;
import com.expedia.gps.geo.reactive101.client.type.CallSuccess;
import com.expedia.gps.geo.reactive101.client.type.SimpleResponse;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;

/**
 * An async HTTP client.
 *
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
public class RXAsyncRESTClient {

  private AsyncHttpClient asyncHttpClient = new AsyncHttpClient(
      new AsyncHttpClientConfig.Builder()
          .setMaxConnections(100)
          .setMaxConnectionsPerHost(100)
          .build());

  public Observable<SimpleResponse> callAsync(String host, String url) throws Exception {
    String finalURL = "http://" + host + url;
    Future<SimpleResponse> f = asyncHttpClient.prepareGet(finalURL).execute(
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
    return Observable.from(f);
  }

}
