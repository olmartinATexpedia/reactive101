package com.expedia.gps.geo.reactive101.client.type;

import lombok.Getter;

/**
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
@Getter
public class CallSuccess implements SimpleResponse {
  private String content;
  public CallSuccess(String content) {
    this.content = content;
  }
}
