package com.nelly.core.domain

import org.joda.time.DateTime

import scala.util.{Success, Try}


case class LogEntry(
                    ip: String,
                    receivedTime: DateTime,
                    requestUrl: String,
                    status: Int,
                    identity: Option[String] = None,
                    userId : Option[String] = None,
                    responseSize: Option[Long] = None) {
  lazy val requestParts =  Try(new java.net.URL(requestUrl)) match {
    case Success(url) =>  url.getPath.split(" ")
    case _ =>  requestUrl.split(" ")
  }
  def requestMethod :String  = requestParts(0)
  def urlSection :String = requestParts(1).replaceAll("/", "")
}

