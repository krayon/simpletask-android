package nl.mpcjanssen.simpletask

import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.util.DateTools

/**
  * Created by Mark on 25-11-2015.
  */
object Token {
  val completed = "completed"
  val completedDate = "completedDate"
  val ttext = "text"
  val hidden = "hidden"
}

abstract class Token {

  def ttype: String
  def text: String
  def value: Any
}

case class Completed(value: Boolean) extends Token {
  override def text: String = if (value) "x" else ""

  override def ttype: String = Token.completed
}

case class CompletedDate(text: String) extends Token {
  override def value: DateTime = {
    DateTools.fromString(text)
  }

  override def ttype: String = Token.completedDate
}

case class CreatedDate(text: String, dateText: String) extends Token {
  override def value: DateTime = {
    DateTools.fromString(dateText)
  }

  override def ttype: String = Token.completedDate
}

case class Hidden(text: String) extends Token {
  override def ttype: String = Token.hidden
  override def value: Boolean = true
}

case class TText(text: String) extends Token {
  override def ttype: String = Token.ttext

  override def value: String = text
}


