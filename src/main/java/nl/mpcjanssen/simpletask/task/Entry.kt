package nl.mpcjanssen.simpletask.task

import com.orm.SugarRecord

class Entry (line: Long, var task: Task) : SugarRecord() {
    var line = line
    var text = task.inFileFormat()
}