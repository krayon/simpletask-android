/**
  * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
  *
  * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
  *
  * LICENSE:
  *
  * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
  * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
  * later version.
  *
  * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
  * details.
  *
  * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
  * <http://www.gnu.org/licenses/>.
  *
  * @author Todo.txt contributors <todotxt@yahoogroups.com>
  * @author Mark Janssen
  */
package nl.mpcjanssen.simpletask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import android.widget.Button


class LoginActivity extends ThemedActivity {
  val TAG = this.getClass.getSimpleName
  private var m_app: TodoApplication = null
  private var m_broadcastReceiver: BroadcastReceiver = null
  private var localBroadcastManager: LocalBroadcastManager = null

  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    m_app = getApplication.asInstanceOf[TodoApplication]
    setTheme(m_app.getActiveTheme)
    setContentView(R.layout.login)
    localBroadcastManager = LocalBroadcastManager.getInstance(this)
    val intentFilter: IntentFilter = new IntentFilter
    intentFilter.addAction("nl.mpcjanssen.simpletask.ACTION_LOGIN")
    m_broadcastReceiver = new BroadcastReceiver() {
      def onReceive(context: Context, intent: Intent) {
        val i: Intent = new Intent(context, classOf[Simpletask])
        startActivity(i)
        finish()
      }
    }
    localBroadcastManager.registerReceiver(m_broadcastReceiver, intentFilter)
    var m_LoginButton: Button = findViewById(R.id.login).asInstanceOf[Button]
    m_LoginButton.setOnClickListener(new View.OnClickListener() {
      def onClick(v: View) {
        m_app.setFullDropboxAccess(true)
        startLogin()
      }
    })
    m_LoginButton = findViewById(R.id.login_folder).asInstanceOf[Button]
    m_LoginButton.setOnClickListener(new View.OnClickListener() {
      def onClick(v: View) {
        m_app.setFullDropboxAccess(false)
        startLogin()
      }
    })
    if (m_app.isAuthenticated) {
      switchToTodolist()
    }
  }

  private def switchToTodolist () {
    val intent: Intent = new Intent(this, classOf[Simpletask])
    startActivity(intent)
    finish()
  }

  protected override def onResume() {
    super.onResume()
    finishLogin()
  }

  private def finishLogin () {
    if (m_app.isAuthenticated) {
      m_app.fileChanged(null)
      switchToTodolist()
    }
  }

  protected override def onDestroy () {
    super.onDestroy()
    localBroadcastManager.unregisterReceiver(m_broadcastReceiver)
  }

  private[simpletask] def startLogin () {
    m_app.startLogin(this, 0)
  }
}
