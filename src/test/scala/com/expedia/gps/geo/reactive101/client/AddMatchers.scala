package com.expedia.gps.geo.reactive101.client

import org.scalatest.matchers.{BePropertyMatchResult, BePropertyMatcher}

/**
 *
 * @author olmartin@expedia.com
 * @since 2015-11-13
 */
trait AddMatchers {

  def anInstanceOf[T](implicit manifest: Manifest[T]) = {
    val clazz = manifest.runtimeClass.asInstanceOf[Class[T]]
    new BePropertyMatcher[AnyRef] {
      def apply(left: AnyRef) =
        BePropertyMatchResult(left.getClass.isAssignableFrom(clazz), "an instance of " + clazz.getName)
    }
  }
}
