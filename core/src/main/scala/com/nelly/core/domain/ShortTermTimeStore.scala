package com.nelly.core.domain

import java.util.Calendar


/**
 * *
 * @param storeDurationInSeconds number of seconds you want to keep time data
 */
class ShortTermTimeStore(val storeDurationInSeconds: Int) extends LastXTimeStore {
  val xMinutesSize = storeDurationInSeconds
  var milliseconds = new Array[Int](1000)
  val xMinutes = new Array[Int](xMinutesSize)
  var xMinutesIndex = 0
  
  override def secondsTotal(): Int = xMinutes.reduce(_+_)

  private[this] def millisecondsTotal(): Int = milliseconds.reduce(_+_)
  
  override def incrementMillisecondsCount(): Unit = synchronized {
    val calendar = Calendar.getInstance()
    val millis = calendar.get(Calendar.MILLISECOND)
    milliseconds.update(millis, milliseconds(millis)+1)
  }

  private[this] def resetMilliseconds(): Unit = synchronized {
    milliseconds = Array.fill[Int](1000)(0)
  }

  /**
   * * call every second
   */
  override def secondsTick(): Unit = synchronized {
    xMinutes.update(xMinutesIndex, millisecondsTotal())
    xMinutesIndex = (xMinutesIndex + 1)%xMinutesSize
    resetMilliseconds()
   
  }
}
