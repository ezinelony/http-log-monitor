package com.nelly.util

import scala.util.Try


object EnvironmentalConfig extends Config {
  
  val logPath = getStringOrElse("ENV_ACCESS_LOG", "/Users/nelly/Documents/Workspaces/challenges/access_log")
  val alertThreshold =  getIntOrElse("ENV_ALERT_THRESHOLD", 10)
  val alertDelayInSeconds = getIntOrElse("ENV_ALERT_DELAY_IN_SECONDS", 60)
  val sectionTickDurationInSeconds = getIntOrElse("ENV_SECTION_TICK_DURATION_IN_SECONDS", 10)
  val lastAlertCalculationDurationInSeconds = getIntOrElse("ENV_ALERT_WINDOW_IN_MINUTES", 2)
  val screenSize =  getIntOrElse("ENV_SCREEN_WINDOW", 25) 

  override def getIntOrElse(key: String, default: Int): Int = Try(
    sys.env.getOrElse(key, s"$default").toInt).toOption match {
      case Some(th) => th
      case _ => println(s"Using default value ${default}} for $key"); default
    }


  override def getStringOrElse(key: String, default: String): String =  sys.env.getOrElse(key, default)
}
