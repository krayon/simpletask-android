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
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import android.util.Log;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.*;

import nl.mpcjanssen.simpletask.dao.Entry;
import nl.mpcjanssen.simpletask.remote.BackupInterface;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.util.Util;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Implementation of the in memory representation of the todo list
 *
 * @author Mark Janssen

 */
public class TodoList implements TodoListInterface {
    final static String TAG = TodoList.class.getSimpleName();
    private final Logger log;
    private final boolean startLooper;
    private final TodoApplication app;

    @NonNull
    private List<Task> mTasks = new CopyOnWriteArrayList();
    @NonNull
    private List<Task> mSelectedTask = new CopyOnWriteArrayList();
    @Nullable
    private ArrayList<String> mLists = null;
    @Nullable
    private ArrayList<String> mTags = null;
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
                @Override
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


    @Override
    public void add(final Task t, final boolean atEnd) {
        queueRunnable("Add task", new Runnable() {
            @Override
            public void run() {
                log.debug(TAG, "Adding task of length {} into {} atEnd", t.inFileFormat().length(), TodoList.this, atEnd);
                if (atEnd) {
                    mTasks.add(t);
                } else {
                    mTasks.add(0,t);
                }
            }
        });
    }


    @Override
    public void remove(@NonNull final Task t) {
        queueRunnable("Remove", new Runnable() {
            @Override
            public void run() {
                mTasks.remove(t);
            }
        });
    }


    @Override
    public int size() {
        return mTasks.size();
    }

    @Override
    public int find(Task t) {
        if (mTasks == null) {
            return -1;
        }
        return mTasks.indexOf(t);
    }

    @Override
    public Task get(int position) {
        return mTasks.get(position);
    }

    @Override
    @NonNull
    public ArrayList<Priority> getPriorities() {
        Set<Priority> res = new HashSet<>();
        for (Task item : mTasks) {
            res.add(item.getPriority());
        }
        ArrayList<Priority> ret = new ArrayList<>(res);
        Collections.sort(ret);
        return ret;
    }

    @Override
    @NonNull
    public ArrayList<String> getTags() {
        ArrayList<String> result = new ArrayList<String>();
        Cursor cursor = app.db.rawQuery("SELECT DISTINCT text FROM " + app.entryTagDao.getTablename(), null);
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }
        cursor.close();
        return result;
    }

    @Override
    @NonNull
    public ArrayList<String> getLists() {
        ArrayList<String> result = new ArrayList<String>();
        Cursor cursor = app.db.rawQuery("SELECT DISTINCT text FROM " + app.entryListDao.getTablename(), null);
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }
        cursor.close();
        return result;
    }


    @Override
    public ArrayList<String> getDecoratedLists() {
        return Util.prefixItems("@", getLists());
    }

    @Override
    public ArrayList<String> getDecoratedTags() {
        return Util.prefixItems("+", getTags());
    }


    @Override
    public void undoComplete(@NonNull final List<Task> tasks) {
        queueRunnable("Uncomplete", new Runnable() {
            @Override
            public void run() {
                for (Task t : tasks) {
                    t.markIncomplete();
                }
            }
        });
    }

    @Override
    public void complete(@NonNull final Task task,
                         final boolean keepPrio) {

        queueRunnable("Complete", new Runnable() {
            @Override
            public void run() {

                Task extra = task.markComplete(DateTime.now(TimeZone.getDefault()));
                if (extra != null) {
                    mTasks.add(extra);
                }
                if (!keepPrio) {
                    task.setPriority(Priority.NONE);
                }
            }
        });
    }


    @Override
    public void prioritize(final List<Task> tasks, final Priority prio) {
        queueRunnable("Complete", new Runnable() {
            @Override
            public void run() {
                for (Task t : tasks) {
                    t.setPriority(prio);
                }
            }
        });
    }

    @Override
    public void defer(@NonNull final String deferString, @NonNull final Task tasksToDefer, final int dateType) {
        queueRunnable("Defer", new Runnable() {
            @Override
            public void run() {
                switch (dateType) {
                    case Task.DUE_DATE:
                        tasksToDefer.deferDueDate(deferString, Util.getTodayAsString());
                        break;
                    case Task.THRESHOLD_DATE:
                        tasksToDefer.deferThresholdDate(deferString, Util.getTodayAsString());
                        break;
                }
            }
        });
    }

    @Override
    @NonNull
    public List<Task> getSelectedTasks() {
        if (mSelectedTask==null) {
            mSelectedTask = new CopyOnWriteArrayList();
        }
        return mSelectedTask;
    }

    @Override
    public void setSelectedTasks(List<Task> selectedTasks) {
        this.mSelectedTask = selectedTasks;
    }


    @Override
    public void notifyChanged(final FileStoreInterface filestore, final String todoname, final String eol, final BackupInterface backup, final boolean save) {
        log.info(TAG, "Handler: Queue notifychanged");
        todolistQueue.post(new Runnable() {
            @Override
            public void run() {
                if (save) {
                    log.info(TAG, "Handler: Handle notifychanged");
                    log.info(TAG, "Saving todo list, size {}", mTasks.size());
                    save(filestore, todoname, backup, eol);
                }
                clearSelectedTasks();
                if (mTodoListChanged != null) {
                    log.info(TAG, "TodoList changed, notifying listener and invalidating cached values");
                    mTags = null;
                    mLists = null;
                    mTodoListChanged.todoListChanged();
                } else {
                    log.info(TAG, "TodoList changed, but nobody is listening");
                }
            }
        });
    }



    @Override
    public List<Task> getSortedTasksCopy(@NonNull ActiveFilter filter, @NonNull ArrayList<String> sorts, boolean caseSensitive) {
        // Fixme

        return getTasks();
    }

    @Override
    public void selectTask(Task t) {
        if (mSelectedTask.indexOf(t) == -1) {
            mSelectedTask.add(t);
        }
    }

    @Override
    public void unSelectTask(Task t) {
        mSelectedTask.remove(t);
    }

    @Override
    public void clearSelectedTasks() {
        mSelectedTask = new CopyOnWriteArrayList();
    }

    @Override
    public void selectTask(int index) {
        if (index < 0 || index > mTasks.size() - 1) {
            return;
        }
        selectTask(mTasks.get(index));
    }

    @Override
    public void reload(final FileStoreInterface fileStore, final String filename, final BackupInterface backup, final LocalBroadcastManager lbm, final boolean background, final String eol) {
        if (TodoList.this.loadQueued()) {
            log.info(TAG, "Todolist reload is already queued waiting");
            return;
        }
        lbm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
        loadQueued = true;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                clearSelectedTasks();
                Log.i(TAG,"Loading entries into DB");
                try {
                    app.daoSession.callInTx(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            fileStore.loadTasksFromFile(app.entryDao, app.entryListDao, app.entryTagDao, filename, backup, eol);
                            return null;
                        }
                    });
                }
                 catch (Exception e) {
                    Log.e(TAG, "Todolist load failed:" +  filename, e);
                    Util.showToastShort(TodoApplication.getAppContext(), "Loading of todo file failed");
                }
                loadQueued = false;

                Log.i(TAG,"Stored " + app.entryDao.count() + " entries in DB");
                long numLists =  getLists().size();
                long numTags =  getTags().size();
                Log.i(TAG,"Stored " + app.entryDao.count() + " entries in DB");
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

    @Override
    public void save(final FileStoreInterface filestore, final String todoFileName, final BackupInterface backup, final String eol) {
        queueRunnable("Save", new Runnable() {
            @Override
            public void run() {
                try {
                    filestore.saveTasksToFile(todoFileName, mTasks, backup, eol);
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.showToastLong(TodoApplication.getAppContext(), R.string.write_failed);
                }
            }
        });

    }

    @Override
    public void archive(final FileStoreInterface filestore, final String todoFilename, final String doneFileName, final List<Task> tasks, final String eol) {
        queueRunnable("Archive", new Runnable() {
            @Override
            public void run() {
                List<Task> tasksToArchive;
                if (tasks == null) {
                    tasksToArchive = mTasks;
                } else {
                    tasksToArchive = tasks;
                }
                List<Task> tasksToDelete = new ArrayList<>();
                for (Task t : tasksToArchive) {
                    if (t.isCompleted()) {
                        tasksToDelete.add(t);
                    }
                }
                try {
                    filestore.appendTaskToFile(doneFileName, tasksToDelete, eol);
                    for (Task t : tasksToDelete) {
                        mTasks.remove(t);
                    }
                    notifyChanged(filestore, todoFilename, eol, null, true);
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.showToastShort(TodoApplication.getAppContext(), "Task archiving failed");
                }
            }
        });
    }

    @Override
    public void replace(Task old, Task updated) {
        int index = mTasks.indexOf(old);
        if (index>-1) {
            mTasks.set(index,updated);
        } else {
            mTasks.add(updated);
        }
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

        @Override
        public String toString() {
            return description;
        }

        @Override
        public void run() {
            log.info(TAG, "Execution action " + description);
            runnable.run();
        }

    }

    @Override
    public List<Task> getTasks() {
        Log.i(TAG, "Loading tasks from DB");
        ArrayList<Task> tasks = new ArrayList<>();
        for (Entry entry: app.entryDao.loadAll()) {
            tasks.add(new Task(entry.getText()));
        }
        Log.i(TAG, "Got " + tasks.size() + " tasks from DB");
        return tasks;
    }
}
