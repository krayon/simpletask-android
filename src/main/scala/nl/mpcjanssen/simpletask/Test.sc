import nl.mpcjanssen.simpletask.{Token, TTask}

val t = TTask.fromString("x 2014-12-12 xb abcd ed f")

val s = "([0-9]{4})".r.unapplySeq( "2014-")