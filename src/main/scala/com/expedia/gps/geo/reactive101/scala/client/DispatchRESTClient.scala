package com.expedia.gps.geo.reactive101.scala.client

import com.expedia.gps.geo.reactive101.client.`type`.SimpleResponse
import dispatch.{Http, url}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 *
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
class DispatchRESTClient(implicit ec:ExecutionContext) extends ScalaRESTClient{

  def call(host: String, path: String): SimpleResponse = {
    val future = callAsync(host, path)
    Await.result(future, Duration.Inf)
//    Await.result(future, 3 seconds)
  }

  def callAsync(host: String, path: String): Future[SimpleResponse] = {
    Http(url(s"http://$host$path")) map {
      case resp if resp.getStatusCode == 200 =>
        new CallSuccess(resp.getResponseBody)
      case resp =>
        new CallFailure(resp.getStatusCode, resp.getResponseBody)
    }
  }
}

case class CallSuccess(content: String) extends SimpleResponse
case class CallFailure(statusCode: Int, content: String) extends SimpleResponse
