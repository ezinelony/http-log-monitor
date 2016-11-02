package com.nelly.core.domain



case class UrlSection(name: String, hits: Int, statuses: Map[Int, Int], averagePayloadSize: Long) {
  
  override def hashCode(): Int = name.hashCode
  
  //override def equals(a: )
  def +(hit: Int) :UrlSection = copy(name, hits + hit)

  def +(hit: Int, status: (Int, Int), payloadSizeAverage: Long) :UrlSection = {
    val m = Map[Int, Int](status._1 -> (statuses.getOrElse(status._1, 0) + status._2) )
    val newPayloadSizeAverage = (averagePayloadSize*hits + payloadSizeAverage*hit)/(hits+hit)
    copy(name, hits + hit, statuses ++ m, newPayloadSizeAverage)
  }

  /*
   This is a partial equals as it only checks the name property
   */
  override def equals(obj: scala.Any): Boolean = {
    obj.isInstanceOf[UrlSection] match {
      case false => false
      case _ => obj.asInstanceOf[UrlSection].hashCode() == hashCode()
    }
  }
}

object SectionOrdering {
  
  def hitsOrdering: Ordering[UrlSection] = new Ordering[UrlSection] {
    def compare(a: UrlSection, b: UrlSection) = a.hits compare b.hits
    override def toString() : String  = "hits"
  }

  def notFoundOrdering: Ordering[UrlSection] = new Ordering[UrlSection] {
    def compare(a:UrlSection, b:UrlSection) = a.statuses.getOrElse(404, 0) compare b.statuses.getOrElse(404, 0)
    override def toString() : String  = "404s"
  }

  def internalServerOrdering: Ordering[UrlSection] = new Ordering[UrlSection] {
    def compare(a:UrlSection, b:UrlSection) = {

      (a.statuses.groupBy((k) => k._1.toInt > 499).get(true) match {
        case Some(e) => e.values.reduce(_ + _);
        case None => 0
      }) compare ( b.statuses.groupBy((k) => k._1.toInt > 499).get(true) match {
        case Some(e) => e.values.reduce(_ + _);
        case None => 0
      })
    }

    override def toString() : String  = "inter server error"
    
  }
}