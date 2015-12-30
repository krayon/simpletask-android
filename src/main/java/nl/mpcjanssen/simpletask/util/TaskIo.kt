/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 *
 * LICENSE:
 *
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Todo.txt contributors @yahoogroups.com>
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
@file:JvmName("TaskIo")
package nl.mpcjanssen.simpletask.util

import nl.mpcjanssen.simpletask.dao.*
import nl.mpcjanssen.simpletask.task.TodoTxtTask
import java.io.*


private val TAG = "TaskIo"

@Throws(IOException::class)
fun loadDaoFromFile(daos: Daos,  file: File) {
    daos.entryDao.deleteAll();
    daos.listDao.deleteAll();
    daos.tagDao.deleteAll();
    var line = 0
    file.forEachLine {
        TodoTxtTask.addToDatabase(daos,line,it);
        line++;
    }
}

fun getFileContents(file: File) : String {
    return file.readText()
}

@Throws(IOException::class)
fun writeToFile(contents: String, file: File, append: Boolean) {
    createParentDirectory(file)
    val str = FileOutputStream(file, append)

    val fw = BufferedWriter(OutputStreamWriter(
            str, "UTF-8"))
    fw.write(contents)
    fw.close()
    str.close()
}


