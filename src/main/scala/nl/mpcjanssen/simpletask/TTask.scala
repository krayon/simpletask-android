package nl.mpcjanssen.simpletask


case class TTask(text: String) {
  val tokens = TTask.fromString(text)

  def completed:Boolean = {
    tokens.head.asInstanceOf[Completed].value
  }

  def asText = {
    tokens.foldLeft("")((s, t) => s"$s${t.text}")
  }

}

object TTask {

  def extractCompletionAndPrio(lexemes: Seq[String]) : (Seq[Token], Seq[String]) = {
    val rdate = "([0-9]{4}-[0-9]{2}-[0-9]{2})".r
    lexemes match {
      case Seq("x", " ", rdate(d), " ", rest @ _ *)  =>
        (Seq(Completed(true),TText(" "),CompletedDate(d), TText(" ")), rest)
      case Seq(rest @ _ *)  => (Seq(Completed(false)),rest)
    }
  }

  def fromString(text: String): Seq[Token] = {
    val lexemes = text.split("""((?<=[\s.])|(?=[\s.]))""")
    val (completionPrefix, rest) = extractCompletionAndPrio(lexemes)
    rest.foldLeft(completionPrefix){ tokenize(_,_) }
  }

  def tokenize(tokens: Seq[Token], s: String):Seq[Token] = {
    s match {
      case _  => tokens :+ TText(s)
    }
  }
}



