package com.expedia.gps.geo.reactive101.gaia.client

import com.expedia.gps.geo.reactive101.gaia.client.`type`.{SimpleResponse, CallSuccess}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.scalatest.{FlatSpec, Matchers}

/**
 *
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
class RestClientTest extends FlatSpec  with Matchers with AddMatchers {

  val basicClient = new BasicRESTClient()
  val asyncClient = new AsyncRESTClient()
  implicit val formats = DefaultFormats

  "a BasicRESTClient" should "be able to call gaia and retrieve results" in {
    val featureId = 4002L
    val response: SimpleResponse = basicClient.call("gaia.uat.karmalab.net:8100", s"/features/$featureId?cid=1&apk=1")
    response should be (anInstanceOf[CallSuccess])
    val json = parse(response.asInstanceOf[CallSuccess].getContent)
    (json \ "id").extract[String].toLong should equal (featureId)
  }

  "an asyncRESTClient" should "be able to call gaia and retrieve results" in {
    val featureId = 4002L
    val response: SimpleResponse = asyncClient.call("gaia.uat.karmalab.net:8100", s"/features/$featureId?cid=1&apk=1")
    response should be (anInstanceOf[CallSuccess])
    val json = parse(response.asInstanceOf[CallSuccess].getContent)
    (json \ "id").extract[String].toLong should equal (featureId)
  }
}
