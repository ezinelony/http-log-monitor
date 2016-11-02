package com.nelly.core

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat


object Formatter {


  val outDateTime = DateTimeFormat.forPattern("MM/dd/YYYY, h:mm:ss a")
  def timeDiff(previous: DateTime, recent: DateTime) :String = {
    val d = recent.getMillis - previous.getMillis
    var diffInMillis = d / 1000

    val seconds = diffInMillis % 60
    diffInMillis /= 60
    val minutes = diffInMillis % 60
    diffInMillis /= 60
    val hours = diffInMillis % 24
    diffInMillis /= 24
    val days = diffInMillis

    if (d < 1000 ) s"${d} milliseconds" else s" ${days} days, ${hours} hours, ${minutes}  minutes, ${seconds} seconds"

  }
  
  def contentSize(s: Long, unit: String = "sec") : String = {
    if (s < 1000) s"${s}B/${unit}" else if (s < 1024 * 1000) s"${s/1024}kB/${unit}" else s"${s/(1024*1024)}MB/${unit}"
  }
  
  def dateTime(d :DateTime) :String = outDateTime.print(d)

}
