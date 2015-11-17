package nl.mpcjanssen.simpletask.util

import java.io._
import java.util

import android.support.annotation.NonNull

import scala.collection.{JavaConversions, JavaConverters}
import scala.io.{Codec, Source}

object TaskIo {
  @throws(classOf[IOException])
  def loadFromFile(file: File) : util.Collection[String] = {
    val lines = Source.fromFile(file)(Codec.UTF8).getLines().toList;
   JavaConversions.asJavaCollection[String](lines)
  }

  private val TAG: String = TaskIo.getClass.getSimpleName

  @throws(classOf[IOException])
  def writeToFile(@NonNull contents: String, @NonNull file: File, append: Boolean) {
    Util.createParentDirectory(file)
    val str: FileOutputStream = new FileOutputStream(file, append)
    val fw: Writer = new BufferedWriter(new OutputStreamWriter(str, "UTF-8"))
    fw.write(contents)
    fw.close
    str.close
  }
}