package com.expedia.gps.geo.reactive101.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import com.expedia.gps.geo.reactive101.client.type.SimpleResponse;

/**
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
public interface RestClient {

  SimpleResponse call(String host, String url) throws Exception;

  Future<SimpleResponse> callAsync(String host, String url) throws Exception;

  CompletableFuture<SimpleResponse> callAsync2(String host, String url, Executor executor) throws Exception;
}
