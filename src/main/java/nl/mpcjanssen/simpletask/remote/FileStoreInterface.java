package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import nl.mpcjanssen.simpletask.task.Task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Interface definition of the storage backend used.
 *
 * Uses events to communicate with the application. Currently supported are SYNC_START, SYNC_DONE and FILE_CHANGED.
 */
public interface FileStoreInterface {
    boolean isAuthenticated();
<<<<<<< HEAD
    List<Task> loadTasksFromFile(String path, @Nullable BackupInterface backup, String eol)  throws IOException;
=======
    List<Task> loadTasksFromDocument(DocumentFile in, @Nullable BackupInterface backup)  throws IOException;
>>>>>>> origin/extsdwrite
    void startLogin(Activity caller, int i);
<<<<<<< HEAD
    void logout();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly);
<<<<<<< HEAD
    void saveTasksToFile(String path, List<Task> tasks, @Nullable BackupInterface backup, String eol) throws IOException;
    void appendTaskToFile(String path, List<Task> tasks, String eol) throws IOException;
=======
    void saveTasksToDocument(DocumentFile out, List<Task> tasks, @Nullable BackupInterface backup) throws IOException;
    void appendTaskToFile(DocumentFile out, List<Task> tasks) throws IOException;
>>>>>>> origin/extsdwrite

=======
    void finishLogin();
    void deauthenticate();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly);
    void saveTasksToFile(String path, TaskCache taskCache);
    void appendTaskToFile(String path, List<Task> tasks) throws IOException;
>>>>>>> origin/dropbox-change
    int getType();
    void sync();
    String readFile(String file, FileReadListener fileRead) throws IOException;
    boolean supportsSync();

    boolean isLoading();

    boolean changesPending();

    interface FileSelectedListener {
        void fileSelected(String file);
    }
    interface FileChangeListener {
        void fileChanged(String newName);
    }

    interface FileReadListener {
        void fileRead(String contents);
    }
}
