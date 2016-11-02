package com.nelly.util


trait Config {

  def getIntOrElse(key: String, default: Int) : Int
  def getStringOrElse(key: String, default: String) : String
  
}
