/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 * <p/>
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 * <p/>
 * LICENSE:
 * <p/>
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 * <p/>
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.task;

import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import android.util.Log;
import de.greenrobot.dao.query.QueryBuilder;
import nl.mpcjanssen.simpletask.*;

import nl.mpcjanssen.simpletask.dao.Entry;
import nl.mpcjanssen.simpletask.dao.EntryDao;
import nl.mpcjanssen.simpletask.remote.BackupInterface;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.util.Util;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;


/**
 * Implementation of the in memory representation of the todo list
 *
 * @author Mark Janssen

 */
public class TodoList {
    final static String TAG = TodoList.class.getSimpleName();
    private final Logger log;
    private final boolean startLooper;
    private final TodoApplication app;

    private TodoListChanged mTodoListChanged;

    private Handler todolistQueue;
    private boolean loadQueued = false;


    public TodoList(TodoListChanged todoListChanged, TodoApplication app) {
        this(todoListChanged, app, true);
    }


    public TodoList(TodoListChanged todoListChanged, TodoApplication app,
                    boolean startLooper) {
        this.startLooper = startLooper;
        this.app = app;
        // Set up the message queue
        if (startLooper) {
            Thread t = new Thread(new Runnable() {
                
                public void run() {
                    Looper.prepare();
                    todolistQueue = new Handler();
                    Looper.loop();
                }
            });
            t.start();
        }
        log = Logger.INSTANCE;
        this.mTodoListChanged = todoListChanged;


    }


    public void queueRunnable(final String description, Runnable r) {
        log.info(TAG, "Handler: Queue " + description);
        while (todolistQueue==null && startLooper ) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (todolistQueue!=null) {
            todolistQueue.post(new LoggingRunnable(description, r));
        } else {
            r.run();
        }
    }

    public boolean loadQueued() {
        return loadQueued;
    }


    
    public void add(final Task t, final boolean atEnd) {

        queueRunnable("Add task", new Runnable() {
            
            public void run() {
                log.debug(TAG, "Adding task of length {} into {} atEnd", t.inFileFormat().length(), TodoList.this, atEnd);
                try {
                    app.daoSession.callInTx(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            EntryDao dao = app.daos.getEntryDao();
                            int line;
                            if (atEnd) {
                                Cursor c = app.db.rawQuery("SELECT max(line) FROM " + dao.getTablename(),null);
                                c.moveToFirst();
                                line = c.getInt(0);
                                line++;
                                c.close();

                            } else {
                                Cursor c = app.db.rawQuery("SELECT min(line) FROM " + dao.getTablename(),null);
                                c.moveToFirst();
                                line = c.getInt(0);
                                line--;
                                c.close();

                            }
                            TodoTxtTask.addToDatabase(app.daos,line,t.inFileFormat());

                            return null;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }


    
    public void remove(@NonNull final Task t) {
        queueRunnable("Remove", new Runnable() {
            
            public void run() {
                //Fixme
            }
        });
    }


    
    public long size() {
        return app.daos.getEntryDao().count();
    }

    
    public int find(Task t) {
        // Fixme
        return -1;
    }

    
    public Task get(int position) {
        // Fixme
        return null;
    }

    
    @NonNull
    public ArrayList<Priority> getPriorities() {
        // Fixme
        return new ArrayList<>();
    }

    
    @NonNull
    public SortedSet<String> getTags() {
        SortedSet<String> result = new TreeSet<>();
        QueryBuilder<Entry> qb = app.daos.getEntryDao().queryBuilder();
        for (Entry e : qb.list()) {
            result.addAll(e.getTags().getSorted());
        }
        return result;
    }

    @NonNull
    public SortedSet<String> getLists() {
        SortedSet<String> result = new TreeSet<>();
        QueryBuilder<Entry> qb = app.daos.getEntryDao().queryBuilder();
        for (Entry e : qb.list()) {
            result.addAll(e.getLists().getSorted());
        }
        return result;
    }


    
    public ArrayList<String> getDecoratedLists() {
        return Util.prefixItems("@", new ArrayList<>(getLists()));
    }

    
    public ArrayList<String> getDecoratedTags() {
        return Util.prefixItems("@", new ArrayList<>(getTags()));
    }


    

    



    


    


    
    public void notifyChanged(final FileStoreInterface filestore, final String todoname, final String eol, final BackupInterface backup, final boolean save) {
        log.info(TAG, "Handler: Queue notifychanged");
        todolistQueue.post(new Runnable() {
            
            public void run() {
                if (save) {
                    log.info(TAG, "Handler: Handle notifychanged");
                    log.info(TAG, "Saving todo list, size {}", app.daos.getEntryDao().count());
                    save(filestore, todoname, backup, eol);
                }
                clearSelectedTasks();
                if (mTodoListChanged != null) {
                    log.info(TAG, "TodoList changed, notifying listener and invalidating cached values");
                    mTodoListChanged.todoListChanged();
                } else {
                    log.info(TAG, "TodoList changed, but nobody is listening");
                }
            }
        });
    }



    
    public QueryBuilder<Entry> getSortedTasksQueryBuilder(@NonNull ActiveFilter filter, @NonNull ArrayList<String> sorts, boolean caseSensitive) {
        // Fixme

        return app.daos.getEntryDao().queryBuilder().orderAsc(EntryDao.Properties.Lists);
    }






    
    public void reload(final FileStoreInterface fileStore, final String filename, final BackupInterface backup, final LocalBroadcastManager lbm, final boolean background, final String eol) {
        if (TodoList.this.loadQueued()) {
            log.info(TAG, "Todolist reload is already queued waiting");
            return;
        }
        lbm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
        loadQueued = true;
        Runnable r = new Runnable() {
            
            public void run() {
                clearSelectedTasks();
                Log.i(TAG,"Loading entries into DB");
                try {
                    app.daoSession.callInTx(new Callable<Object>() {
                        
                        public Object call() throws Exception {
                            fileStore.loadTasksFromFile(app.daos, filename, backup, eol);
                            return null;
                        }
                    });
                }
                 catch (Exception e) {
                    Log.e(TAG, "Todolist load failed:" +  filename, e);
                    Util.showToastShort(TodoApplication.getAppContext(), "Loading of todo file failed");
                }
                loadQueued = false;

                Log.i(TAG,"Stored " + app.daos.getEntryDao().count() + " entries in DB");
                long numLists =  getLists().size();
                long numTags =  getTags().size();
                Log.i(TAG,"Stored " + app.daos.getEntryDao().count() + " entries in DB");
                Log.i(TAG,"" + numLists + " distinct lists and " + numTags + " distinct tags" );

                Log.i(TAG,"Todolist loaded, refresh UI");

                notifyChanged(fileStore,filename,eol,backup, false);
            }};
        if (background ) {
            log.info(TAG, "Loading todolist asynchronously into {}", this);
            queueRunnable("Reload", r);

        } else {
            log.info(TAG, "Loading todolist synchronously into {}", this);
            r.run();
        }
    }

    
    public void save(final FileStoreInterface filestore, final String todoFileName, final BackupInterface backup, final String eol) {
        queueRunnable("Save", new Runnable() {
            
            public void run() {
                try {
                    filestore.saveTasksToFile(todoFileName, app.daos.getEntryDao(), backup, eol);
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.showToastLong(TodoApplication.getAppContext(), R.string.write_failed);
                }
            }
        });

    }

    public interface TodoListChanged {
        void todoListChanged();
    }

    public class LoggingRunnable implements Runnable {
        private final String description;
        private final Runnable runnable;

        LoggingRunnable(String description, Runnable r) {
            log.info(TAG, "Creating action " + description);
            this.description = description;
            this.runnable = r;
        }

        
        public String toString() {
            return description;
        }

        
        public void run() {
            log.info(TAG, "Execution action " + description);
            runnable.run();
        }

    }

    
    public List<Task> getTasks() {
        Log.i(TAG, "Loading tasks from DB");
        ArrayList<Task> tasks = new ArrayList<>();
        for (Entry entry: app.daos.getEntryDao().loadAll()) {
            tasks.add(new Task(entry.getText()));
        }
        Log.i(TAG, "Got " + tasks.size() + " tasks from DB");
        return tasks;
    }

    private void setSelectTask(Task t, boolean select) {
        // Fixme try to remove this and replace with selectEntry
        List<Entry> items = app.daos.getEntryDao().queryBuilder().where(EntryDao.Properties.Text.eq(t.inFileFormat())).limit(1).list();
        if (items.size()>0) {
            Entry item = items.get(0);
            item.setSelected(select);
            app.daos.getEntryDao().insertOrReplace(item);
        }

    }

    
    public void selectTask(Task t) {
        setSelectTask(t, true);
    }

    
    public void unSelectTask(Task t) {
        setSelectTask(t, false);
    }

    
    public void selectAllTasks() {
        setSelectAllTasks(true);
    }

    
    public void clearSelectedTasks() {
        setSelectAllTasks(false);
    }

    private void setSelectAllTasks(boolean select) {
        List<Entry> entries = app.daos.getEntryDao().loadAll();
        for (Entry e : entries) {
            e.setSelected(select);
        }
        app.daos.getEntryDao().updateInTx(entries);
    }

    @NonNull
    public List<Entry> getSelectedTasks() {
        return app.daos.getEntryDao().queryBuilder().where(EntryDao.Properties.Selected.eq(true)).list();
    }
}
