package nl.mpcjanssen.simpletask

import android.util.Log

object Logger {
    fun info(tag: String, message: String, vararg items: Any?) : Unit   {
        Log.i(tag,message)
    }

    fun debug(tag: String, message: String, vararg items: Any?) : Unit {
        Log.d(tag,message)
    }
    fun warn(tag: String, message: String, vararg items: Any?) : Unit {
        Log.w(tag,message)
    }
    fun error(tag: String, message: String, vararg items: Any?) : Unit {
        Log.e(tag,message)
    }
    fun error(tag: String, message: String, ex: Exception) : Unit {
        Log.e(tag,message, ex)
    }
}