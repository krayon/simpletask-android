package nl.mpcjanssen.simpletask

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import nl.mpcjanssen.simpletask.dao.DaoMaster
import nl.mpcjanssen.simpletask.dao.DaoSession

class SimpletaskApplication : Application() {
    var daoDb : SQLiteDatabase? = null
    var daoMaster : DaoMaster? = null
    var daoSession: DaoSession? = null

    override fun onCreate() {
        super.onCreate()
        val helper = DaoMaster.DevOpenHelper(this, "entries-db", null)
        daoDb = helper.getWritableDatabase()
        daoMaster = DaoMaster(daoDb)
        daoSession = daoMaster?.newSession()
    }
}