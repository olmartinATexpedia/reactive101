package com.expedia.gps.geo.reactive101.scala.client

import com.expedia.gps.geo.reactive101.gaia.client.`type`.SimpleResponse

import scala.concurrent.Future

/**
 *
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
trait ScalaRESTClient {

  def call(host: String, path: String): SimpleResponse

  def callAsync(host: String, path: String): Future[SimpleResponse]
}
