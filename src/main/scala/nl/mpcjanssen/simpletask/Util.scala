package nl.mpcjanssen.simpletask.util

import java.util.TimeZone

import android.util._
import java.io._
import java.util

import android.support.annotation.NonNull
import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.task.Task
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, JavaConverters}
import scala.io.{Codec, Source}

object Io {

  val TAG = Io.getClass.getName

  @throws(classOf[IOException])
  def loadFromFile(file: File) : util.Collection[String] = {
    Log.d(TAG, s"Reading file ${file.getAbsolutePath}")
    val lines = Source.fromFile(file)(Codec.UTF8).getLines().toList
    Log.d(TAG, s"Read ${lines.size} lines")
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
    folder.mkdirs()
  }
}

object DateTools {
  def fromString(dateString: String) = {
    if (dateString != null && DateTime.isParseable(dateString)) {
      new DateTime(dateString);
    } else if (dateString == null) {
      DateTime.today(TimeZone.getDefault());
    } else {
      null;
    }
  }
}


object Mappings {
  @NonNull def prefixItems(prefix: String, @NonNull items: util.Collection[String]): util.ArrayList[String] = {
    val scalaItems = JavaConversions.collectionAsScalaIterable(items)
    val result = new util.ArrayList[String]()
    result.addAll(JavaConversions.asJavaCollection[String](scalaItems.map(prefix + _)))
    result
  }

  def makeTasks(items: util.Collection[String]): util.Collection[Task] = {
    val scalaItems = JavaConversions.collectionAsScalaIterable(items)
    val result = new util.ArrayList[Task]()
    result.addAll(JavaConversions.asJavaCollection[Task](scalaItems.map(new Task(_))))
    result
  }
}
