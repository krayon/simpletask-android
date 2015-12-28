package nl.mpcjanssen.simpletask.task;

import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.remote.BackupInterface;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pa54518 on 28-12-2015.
 */
public interface TodoListInterface {
    void add(Task t, boolean atEnd);

    void remove(@NonNull Task t);

    int size();

    int find(Task t);

    Task get(int position);

    @NonNull
    ArrayList<Priority> getPriorities();

    @NonNull
    ArrayList<String> getContexts();

    @NonNull
    ArrayList<String> getProjects();

    ArrayList<String> getDecoratedContexts();

    ArrayList<String> getDecoratedProjects();

    void undoComplete(@NonNull List<Task> tasks);

    void complete(@NonNull Task task,
                  boolean keepPrio);

    void prioritize(List<Task> tasks, Priority prio);

    void defer(@NonNull String deferString, @NonNull Task tasksToDefer, int dateType);

    @NonNull
    List<Task> getSelectedTasks();

    void setSelectedTasks(List<Task> selectedTasks);

    void notifyChanged(FileStoreInterface filestore, String todoname, String eol, BackupInterface backup, boolean save);

    List<Task> getTasks();

    List<Task> getSortedTasksCopy(@NonNull ActiveFilter filter, @NonNull ArrayList<String> sorts, boolean caseSensitive);

    void selectTask(Task t);

    void unSelectTask(Task t);

    void clearSelectedTasks();

    void selectTask(int index);

    void reload(FileStoreInterface fileStore, String filename, BackupInterface backup, LocalBroadcastManager lbm, boolean background, String eol);

    void save(FileStoreInterface filestore, String todoFileName, BackupInterface backup, String eol);

    void archive(FileStoreInterface filestore, String todoFilename, String doneFileName, List<Task> tasks, String eol);

    void replace(Task old, Task updated);
}
