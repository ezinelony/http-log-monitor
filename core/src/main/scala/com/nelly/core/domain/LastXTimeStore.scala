package com.nelly.core.domain


trait LastXTimeStore {
  
  val storeDurationInSeconds: Int
  def secondsTotal(): Int
  def incrementMillisecondsCount(): Unit
  def secondsTick(): Unit
}
