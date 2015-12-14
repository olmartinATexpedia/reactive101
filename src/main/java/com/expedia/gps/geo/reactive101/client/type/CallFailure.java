package com.expedia.gps.geo.reactive101.client.type;

import lombok.Getter;

/**
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
@Getter
public class CallFailure implements SimpleResponse {
  private int    statusCode;
  private String errorMessage;

  public CallFailure(int statusCode, String errorMessage) {
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
  }
}
