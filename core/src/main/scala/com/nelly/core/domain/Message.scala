package com.nelly.core.domain


class Message(val message: String)

case class AlertMessage(override val message: String) extends Message(message: String)

case class StatsMessage(override val message: String) extends Message(message: String)

