package com.expedia.gps.geo.reactive101.gaia.client;

import java.util.concurrent.Future;

import com.expedia.gps.geo.reactive101.gaia.client.type.SimpleResponse;

/**
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
public interface RestClient {

  SimpleResponse call(String host, String url) throws Exception;

  Future<SimpleResponse> callAsync(String host, String url) throws Exception;
}
