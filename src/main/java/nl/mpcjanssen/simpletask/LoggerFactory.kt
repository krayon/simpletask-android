@file:JvmName("LoggerFactory")
package nl.mpcjanssen.simpletask

import android.util.Log

fun getLogger(klass: Class <out Any>) : Logger {
    return Logger(klass.simpleName)
}

fun getLogger(tag: String) : Logger {
    return Logger(tag)
}

class Logger(val tag : String) {

    fun info (message : String, vararg items: Any?) =  {
        Log.d(tag, message + items.joinToString(", "))
    }

    fun debug (message : String, vararg items: Any?) =  {
        Log.d(tag, message + items.joinToString(", "))
    }

    fun error (message : String, vararg items: Any?) =  {
        Log.d(tag, message + items.joinToString(", "))
    }

    fun warn (message : String, vararg items: Any?) =  {
        Log.w(tag, message + items.joinToString(", "))
    }
}
