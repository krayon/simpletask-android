package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.jsonextract.JsonExtractionException;
import com.dropbox.client2.jsonextract.JsonMap;
import com.dropbox.client2.jsonextract.JsonThing;
import com.dropbox.client2.session.AppKeyPair;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskCache;
import nl.mpcjanssen.simpletask.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
    private final SharedPreferences mPrefs;
    private String mEol;
    private DropboxAPI<AndroidAuthSession> mDBApi;

    private Context mCtx;
    private String mWatchedFile;
    private String latestCursor;
    private AsyncTask<Void, Void, Void> pollingTask;

    public FileStore( Context ctx, String eol) {
        mPrefs = ctx.getSharedPreferences("filestore", Context.MODE_PRIVATE);
        mCtx = ctx;
        mEol = eol;
        setDbxAPI();
    }

    public static String getDefaultPath() {
        return "/todo/todo.txt";
    }

    private void setDbxAPI () {
        String app_secret = mCtx.getString(R.string.dropbox_consumer_secret);
        String app_key = mCtx.getString(R.string.dropbox_consumer_key).replace("db-", "");
        String savedAuth = mPrefs.getString("token", null);
        AppKeyPair appKeys = new AppKeyPair(app_key, app_secret);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, savedAuth);
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
                mPrefs.edit().putString("token", accessToken).apply();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    private void startWatching(final String path) {
        mWatchedFile = path;
        if (pollingTask == null) {
            Log.v(TAG, "Initializing slow polling thread");
            try {
                while (true) {
                    DropboxAPI.DeltaPage delta = mDBApi.delta(latestCursor);
                    if (delta.hasMore) {
                        latestCursor = delta.cursor;
                        continue;
                    }
                    break;
                }
            } catch (DropboxException e) {
                e.printStackTrace();
            }
            startLongPoll();
        }
    }


    private void startLongPoll ()  {
        pollingTask = new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... v) {
                try {
                    Log.v(TAG, "Long polling");
                    ArrayList<String> params = new ArrayList<>();
                    params.add("cursor");
                    params.add(latestCursor);
                    params.add("timeout");
                    params.add("120");
                    Object response = RESTUtility.request(RESTUtility.RequestMethod.GET, "api-notify.dropbox.com", "longpoll_delta", 1, params.toArray(new String[0]), mDBApi.getSession());
                    Log.v(TAG, "Longpoll response: " + response.toString());
                    JsonThing result = new JsonThing(response);
                    JsonMap resultMap = result.expectMap();
                    boolean changes = resultMap.get("changes").expectBoolean();
                    JsonThing backoff =  resultMap.getOrNull("backoff");
                    Log.v(TAG, "Longpoll ended, changes " + changes + " backoff " + backoff);
                    if (changes) {
                        DropboxAPI.DeltaPage<DropboxAPI.Entry> delta = mDBApi.delta(latestCursor);
                        latestCursor = delta.cursor;
                        for (DropboxAPI.DeltaEntry entry : delta.entries) {
                            if (entry.lcPath.equalsIgnoreCase(mWatchedFile)) {
                                Log.v (TAG, "File " + mWatchedFile + " changed, reloading");
                                LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_FILE_CHANGED));
                                return null;
                            }
                        }
                    }
                } catch (DropboxException e) {
                    e.printStackTrace();
                } catch (JsonExtractionException e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void v) {
                startLongPoll();
            }
        }.execute();
    }

    @Override
    public void deauthenticate() {
        mDBApi.getSession().unlink();
        mPrefs.edit().remove("token").commit();
        mDBApi = null;
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
        private HashMap<String,DropboxAPI.Entry> entryHash = new HashMap<>();
        private File currentPath;

        private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileSelectedListener>();
        private final Activity activity;
        private boolean txtOnly;
        Dialog dialog;

        /**
         * @param activity
         * @param pathName
         */
        public FileDialog(Activity activity, String pathName, boolean txtOnly) {
            this.activity = activity;
            this.txtOnly=txtOnly;
            currentPath = new File(pathName);

        }

        /**
         *
         */
        public void createFileDialog(final Context ctx, final FileStoreInterface fs) {

            final DropboxAPI<AndroidAuthSession> api = ((FileStore)fs).mDBApi;
            if (api==null) {
                return;
            }
            new AsyncTask<Void,Void, AlertDialog.Builder>() {

                @Override
                protected AlertDialog.Builder doInBackground(Void... params) {
                    loadFileList(api, currentPath);
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(currentPath.getPath());

                    builder.setItems(fileList, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String fileChosen = fileList[which];
                            if (fileChosen.equals(PARENT_DIR)) {
                                currentPath = new File(currentPath.getParent());
                                createFileDialog(ctx, fs);
                                return;
                            }
                            File chosenFile = getChosenFile(fileChosen);
                            Log.w("FileStore", "Selected file " + chosenFile.getName());
                            DropboxAPI.Entry entry = entryHash.get(fileChosen);
                            if (entry.isDir) {
                                currentPath = chosenFile;
                                createFileDialog(ctx, fs);
                            } else {
                                dialog.cancel();
                                dialog.dismiss();
                                fireFileSelectedEvent(chosenFile);
                            }
                        }
                    });
                    return builder;
                }

                @Override
                protected void onPostExecute(AlertDialog.Builder builder) {
                    if (dialog!=null) {
                        dialog.cancel();
                        dialog.dismiss();
                    }
                    dialog = builder.create();
                    dialog.show();
                }

            }.execute();
        }


        public void addFileListener(FileSelectedListener listener) {
            fileListenerList.add(listener);
        }


        private void fireFileSelectedEvent(final File file) {
            fileListenerList.fireEvent(new ListenerList.FireHandler<FileSelectedListener>() {
                public void fireEvent(FileSelectedListener listener) {
                    listener.fileSelected(file.toString());
                }
            });
        }

        private DropboxAPI.Entry getPathMetaData(DropboxAPI api, File path) throws DropboxException {
            if (api!=null) {
                return api.metadata(path.toString(), 0, null, true, null);
            } else {
                return null;
            }
        }

        private void loadFileList(DropboxAPI<AndroidAuthSession> api, File path) {
            if (path==null) {
                path = new File("/");
            }
            this.currentPath = path;
            List<String> f = new ArrayList<String>();
            List<String> d = new ArrayList<String>();

            try {
                DropboxAPI.Entry entries = getPathMetaData(api,path) ;
                entryHash.clear();
                if (!entries.isDir) return;
                if (!path.toString().equals("/")) {
                    d.add(PARENT_DIR);
                }
                for (DropboxAPI.Entry entry : entries.contents) {
                    if (entry.isDeleted) continue;
                    if (entry.isDir) {
                        d.add(entry.fileName());
                    } else {
                        f.add(entry.fileName());
                    }
                    entryHash.put(entry.fileName(), entry);
                }
            } catch (DropboxException e) {
                Log.w("FileStore", "Couldn't load list from " + path.getName() + " loading root instead.");
                loadFileList(api, null);
                return;
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
