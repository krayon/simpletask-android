package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.SimpletaskException;
import nl.mpcjanssen.simpletask.util.ListenerList;
import nl.mpcjanssen.simpletask.util.Strings;

/**
 * FileStore implementation backed by Dropbox
 */
public class FileStore implements FileStoreInterface {

    private final String TAG = getClass().getName();
    private String mEol;
    private DropboxAPI<AndroidAuthSession> mDBApi;

    private Context mCtx;

    public FileStore( Context ctx, String eol) {
        mCtx = ctx;
        mEol = eol;
        setDbxAPI();
    }

    public static String getDefaultPath() {
        return "/todo/todo.txt";
    }

    private void setDbxAPI () {
        String app_secret = mCtx.getString(R.string.dropbox_consumer_secret);
        String app_key = mCtx.getString(R.string.dropbox_consumer_key).replace("db-","");
        AppKeyPair appKeys = new AppKeyPair(app_key, app_secret);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
    }


    @Override
    public boolean isAuthenticated() {
        return mDBApi.getSession().isLinked();
    }

    @Override
    public void loadTasksFromFile(final String path, final TaskCache taskCache) throws IOException {
        if (!isAuthenticated()) {
            return;
        }
        new AsyncTask<String, Void, Void> () {
            @Override
            protected Void doInBackground(String... params) {
                Log.v(TAG, "Loading file in background");
                LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
                taskCache.startLoading();
                int i = 0;
                for (String line : readFile(path).split("\r\n|\r|\n")) {
                    taskCache.load(new Task(i, line));
                    i++;
                }
                taskCache.endLoading();
                startWatching(path);
                LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
                LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_UPDATE_UI));
                return null;
            }
        }.execute(path);

    }


    @Override
    public void startLogin(Activity caller, int i) {
        // MyActivity below should be your activity class name
        mDBApi.getSession().startOAuth2Authentication(caller);
    }

    @Override
    public void finishLogin() {
        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    private void startWatching(final String path) {
        // some longpolling voodoo here
    }

    private void stopWatching(String path) {


    }

    @Override
    public void deauthenticate() {
        mDBApi.getSession().unlink();
    }

    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly) {
        FileDialog dialog = new FileDialog(act, path , true);
        dialog.addFileListener(listener);
        dialog.createFileDialog(mCtx, this);
    }

    @Override
    public void saveTasksToFile(String path, TaskCache taskCache) {
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
        stopWatching(path);
        startWatching(path);
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
    }

    @Override
    public void appendTaskToFile(String path, List<Task> tasks) throws IOException {
        throw new SimpletaskException("append not implemented");
    }


    @Override
    public void setEol(String eol) {
        mEol = eol;
    }

    @Override
    public void sync() {
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_FILE_CHANGED));
    }

    @Override
    public String readFile(String fileName) {
        if (fileName==null) {
            return "";
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DropboxAPI.DropboxFileInfo info = mDBApi.getFile(fileName, null, outputStream, null);
            Log.i(TAG, "The " + fileName + " file's rev is: " + info.getMetadata().rev);
            String contents = outputStream.toString("UTF-8");
            outputStream.close();
            return contents;
        } catch (DropboxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public boolean supportsSync() {
        return true;
    }

    @Override
    public int getType() {
        return Constants.STORE_DROPBOX;
    }


    public static class FileDialog {
        private static final String PARENT_DIR = "..";
        private String[] fileList;
        private File currentPath;

        private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileSelectedListener>();
        private final Activity activity;
        private boolean txtOnly;
        File path;

        /**
         * @param activity
         * @param pathName
         */
        public FileDialog(Activity activity, String pathName, boolean txtOnly) {
            this.activity = activity;
            this.txtOnly=txtOnly;
            path = new File(pathName);

        }

        /**
         * @return file dialog
         */
        public Dialog createFileDialog(Context ctx, FileStoreInterface fs) {
            Dialog dialog;
            final DropboxAPI<AndroidAuthSession> api = ((FileStore)fs).mDBApi;
            if (api==null) {
                return null;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            loadFileList(api,path);
            builder.setTitle(currentPath.getPath());

            builder.setItems(fileList, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String fileChosen = fileList[which];
                    File chosenFile = getChosenFile(fileChosen);
                    if (chosenFile.isDirectory()) {
                        loadFileList(api,chosenFile);
                        dialog.cancel();
                        dialog.dismiss();
                        showDialog();
                    } else fireFileSelectedEvent(chosenFile);
                }
            });

            dialog = builder.show();
            return dialog;
        }


        public void addFileListener(FileSelectedListener listener) {
            fileListenerList.add(listener);
        }

        /**
         * Show file dialog
         */
        public void showDialog() {
            createFileDialog(null,null).show();
        }

        private void fireFileSelectedEvent(final File file) {
            fileListenerList.fireEvent(new ListenerList.FireHandler<FileSelectedListener>() {
                public void fireEvent(FileSelectedListener listener) {
                    listener.fileSelected(file.toString());
                }
            });
        }

        private void loadFileList(DropboxAPI<AndroidAuthSession> api, File path) {
            this.currentPath = path;
            List<String> f = new ArrayList<String>();
            List<String> d = new ArrayList<String>();
            if (!path.toString().equals("/")) d.add(PARENT_DIR);
            try {
                DropboxAPI.Entry entries = api.metadata(path.getAbsolutePath(), 0, null, true, null);
                if (!entries.isDir) return;
                for (DropboxAPI.Entry entry : entries.contents) {
                    if (entry.isDeleted) continue;
                    if (entry.isDir) {
                        d.add(entry.path);
                    } else {
                        f.add(entry.path);
                    }
                }
            } catch (DropboxException e) {
                e.printStackTrace();
            }
            Collections.sort(d);
            Collections.sort(f);
            d.addAll(f);
            fileList = d.toArray(new String[d.size()]);
        }

        private File getChosenFile(String fileChosen) {
            if (fileChosen.equals(PARENT_DIR)) return currentPath.getParentFile();
            else return new File(currentPath, fileChosen);
        }
    }



    @Override
    public boolean initialSyncDone() {
        return true;
    }
}
