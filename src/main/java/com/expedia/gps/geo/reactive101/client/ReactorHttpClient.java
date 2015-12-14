package com.expedia.gps.geo.reactive101.client;

import reactor.io.codec.StringCodec;
import reactor.io.net.NetStreams;
import reactor.io.net.http.HttpClient;
import reactor.io.net.impl.netty.http.NettyHttpClient;
import reactor.rx.Promise;

import com.expedia.gps.geo.reactive101.client.type.CallFailure;
import com.expedia.gps.geo.reactive101.client.type.CallSuccess;
import com.expedia.gps.geo.reactive101.client.type.SimpleResponse;

/**
 * @author olmartin@expedia.com
 * @since 2015-11-17
 */
public class ReactorHttpClient {

  public HttpClient<String, String> createClient(String host){
    HttpClient<String, String> client = NetStreams.httpClient(NettyHttpClient.class, t ->
        t.codec(new StringCodec()).connect(host.substring(0, host.indexOf(':')), Integer.valueOf(host.substring(host.indexOf(':') + 1))));
    return client;
  }

  public Promise<SimpleResponse> callAsync(String host, String url) throws Exception {
    HttpClient<String, String> client = createClient(host);
    return client.get(url)
        .flatMap(s ->
                s.toList().map(strings -> {
                  int code = s.responseStatus().getCode();
                  StringBuilder builder = new StringBuilder();
                  for (String aString : strings) {
                    builder.append(aString);
                  }
                  if (code == 200) {
                    return new CallSuccess(builder.toString());
                  } else {
                    return new CallFailure(code, builder.toString());
                  }
                })
        );
  }

  public static void main(String[] args) throws Exception {
    SimpleResponse simpleResponse = new ReactorHttpClient().callAsync("localhost:4200", "/food/takeOrder").await();
    if (simpleResponse instanceof CallSuccess) {
      System.out.println(((CallSuccess) simpleResponse).getContent());
    }
  }
}
