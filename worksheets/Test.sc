import nl.mpcjanssen.simpletask.{Token, TTask}

val t = TTask("x 2014-12-12 h:1 xbabcd ed f").tokens

val s = "([0-9]{4})".r.unapplySeq( "2014-").getOrElse(List.empty)