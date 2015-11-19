package nl.mpcjanssen.simpletask.util

import java.io._
import java.util
import android.util._

import android.support.annotation.NonNull
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, JavaConverters}
import scala.io.{Codec, Source}

object Io {

  @throws(classOf[IOException])
  def loadFromFile(file: File) : util.Collection[String] = {
    Log.d("XXXXXXXXXXXXXX", s"Reading file ${file.getAbsolutePath}")
    val lines = Source.fromFile(file)(Codec.UTF8).getLines().toList
    Log.d("XXXXXXXXXXXXXX", s"Read ${lines.size} lines")
    JavaConversions.asJavaCollection[String](lines)
  }

  @throws(classOf[IOException])
  def writeToFile(@NonNull contents: String, @NonNull file: File, append: Boolean) {
    Folders.create(file.getParentFile.getAbsoluteFile)
    val str: FileOutputStream = new FileOutputStream(file, append)
    val fw: Writer = new BufferedWriter(new OutputStreamWriter(str, "UTF-8"))
    fw.write(contents)
    fw.close()
    str.close()
  }
}

object Folders {
  def create(folder: File): Boolean = {
    folder.mkdirs();
  }
}


object Mappings {
  @NonNull def prefixItems(prefix: String, @NonNull items: util.Collection[String]): util.ArrayList[String] = {
    val scalaItems = JavaConversions.collectionAsScalaIterable(items)
    val result = new util.ArrayList[String]()
    result.addAll(JavaConversions.asJavaCollection[String](scalaItems.map(prefix + _)))
    result
  }
}
