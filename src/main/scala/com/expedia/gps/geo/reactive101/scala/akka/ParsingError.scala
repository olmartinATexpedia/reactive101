package com.expedia.gps.geo.reactive101.scala.akka

import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
  * TODO. 
  */
object ParsingError {

//  val content = """{"id":"6034362","type":"city","name":"Le Bourget, France","source":{"srcId":"6034362","systemId":300},"position":{"type":"Point","coordinates":[2.42771859999037,48.9363030019178]},"tags":{"geo-admin":{"city":{"id":"553248621587364404"}},"lodging":{"aoiDestinationIntId":{"id":"472386282010445808","value":"512957"}},"score":{"visits":{"id":"553248621592148780","value":"332"},"globalLodgingAdwords":{"id":"553248621539490748","value":"0"}},"viewport":{"hotelDensity":{"id":"553248621557086965","value":"2.412567, 48.929539, 2.444979, 48.94658"}},"publication":{"hotwireHotelListing":{"id":"553248621563786823","value":"hsr"}},"location":{"iso3166-1_alpha-3":{"id":"553248621556119059","value":"FRA"}},"hotwire":{"searchCentre":{"id":"553248621563786824","value":"48.866973;2.31185"}}},"links":{"atlas":[{"isParentOf":{"id":"553248621526046706","featureId":"6109886","featureType":"point_of_interest"}},{"isParentOf":{"id":"111","featureId":"111","featureType":"point_of_interest"}},{"isChildOf":{"id":"553248621526046707","featureId":"179898","featureType":"multi_city_vicinity"}}]},"status":"active","metadata":{"updateTime":1449633630621,"updateTimeString":"2015-12-08T20:00:30Z","updateUser":"prod_user"}}"""
  val content = """{"id":"6034362","type":"city","name":"Le Bourget, France","source":{"srcId":"6034362","systemId":300},"position":{"type":"Point","coordinates":[2.42771859999037,48.9363030019178]},"tags":{"geo-admin":{"city":{"id":"553248621587364404"}},"lodging":{"aoiDestinationIntId":{"id":"472386282010445808","value":"512957"}},"score":{"visits":{"id":"553248621592148780","value":"332"},"globalLodgingAdwords":{"id":"553248621539490748","value":"0"}},"viewport":{"hotelDensity":{"id":"553248621557086965","value":"2.412567, 48.929539, 2.444979, 48.94658"}},"publication":{"hotwireHotelListing":{"id":"553248621563786823","value":"hsr"}},"location":{"iso3166-1_alpha-3":{"id":"553248621556119059","value":"FRA"}},"hotwire":{"searchCentre":{"id":"553248621563786824","value":"48.866973;2.31185"}}},"links":{"atlas":[{"isParentOf":{"id":"553248621526046706","featureId":"6109886","featureType":"point_of_interest"}},{"isChildOf":{"id":"553248621526046707","featureId":"179898","featureType":"multi_city_vicinity"}}]},"status":"active","metadata":{"updateTime":1449633630621,"updateTimeString":"2015-12-08T20:00:30Z","updateUser":"prod_user"}}"""

  def main(args: Array[String]) {
    implicit val jsonFormats: Formats = DefaultFormats
    val json: JValue = parse(content)
    val id = (json \ "id").extract[String].toLong
    val name = (json \ "name").extract[String]
    val featureIds = json \ "links" \ "atlas" \ "isParentOf" \ "featureId"
    val ids = featureIds.extractOpt[Seq[String]].getOrElse(featureIds.extract[String] :: Nil).map(_.toLong)
    println(ids)
  }
}
