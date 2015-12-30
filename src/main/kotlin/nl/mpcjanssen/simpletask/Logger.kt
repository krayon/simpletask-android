package nl.mpcjanssen.simpletask

import android.util.Log

object Logger {
    fun verbose(tag: String, message: String, ex: Exception? = null) : Unit {
        Log.v(tag,message, ex)
    }
    fun debug(tag: String, message: String, ex: Exception? = null) : Unit {
        Log.d(tag,message, ex)
    }
    fun info(tag: String, message: String, ex: Exception? = null) : Unit {
        Log.i(tag,message, ex)
    }
    fun warn(tag: String, message: String, ex: Exception? = null) : Unit {
        Log.w(tag,message, ex)
    }
    fun error(tag: String, message: String, ex: Exception? = null) : Unit {
        Log.e(tag,message, ex)
    }
}