import nl.mpcjanssen.simpletask.{Token, TTask}

val t = TTask("x 2014-12-12xbabcd ed f").completedDate

val s = "([0-9]{4})".r.unapplySeq( "2014-")