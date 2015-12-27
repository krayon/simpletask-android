<<<<<<< HEAD
<<<<<<< HEAD
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
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.util;


import android.app.Activity;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.SparseBooleanArray;
import android.view.Window;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.common.base.Joiner;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.*;
import nl.mpcjanssen.simpletask.sort.AlphabeticalStringComparator;
import nl.mpcjanssen.simpletask.task.Task;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private Util() {
    }

    public static String getTodayAsString() {
        return DateTime.today(TimeZone.getDefault()).format(Constants.DATE_FORMAT);
    }

    public static void runOnMainThread (Runnable r) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(r);
    }


    public static void showToastShort(@NonNull final Context cxt, final int resid) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showToastLong(@NonNull final Context cxt, final int resid) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, resid, Toast.LENGTH_LONG).show();
            }
        });
    }


    public static void showToastShort(@NonNull final Context cxt, final String msg) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showToastLong(@NonNull final Context cxt, final String msg) {
        runOnMainThread (new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    public static List<String> tasksToString(@NonNull List<Task> tasks) {
        ArrayList<String> result = new ArrayList<>();
        for (Task t: tasks) {
            result.add(t.inFileFormat());
        }
        return result;
    }

    public interface InputDialogListener {
        void onClick(String input);
    }

    public static List<VisibleLine> addHeaderLines(List<Task> visibleTasks, String firstSort, String no_header, boolean showHidden, boolean showEmptyLists) {
        String header = "" ;
        String newHeader;
        ArrayList<VisibleLine> result = new ArrayList<>();
        for (Task t : visibleTasks) {
            newHeader = t.getHeader(firstSort, no_header);
            if (!header.equals(newHeader)) {
                VisibleLine headerLine = new HeaderLine(newHeader);
                int last = result.size() - 1;
                if (last != -1 && result.get(last).header() && !showEmptyLists) {
                    // replace empty preceding header
                    result.set(last, headerLine);
                } else {
                    result.add(headerLine);
                }
                header = newHeader;
            }

            if (t.isVisible() || showHidden) {
                // enduring tasks should not be displayed
                VisibleLine taskLine = new TaskLine(t);
                result.add(taskLine);
            }
        }

        // Clean up possible last empty list header that should be hidden
        int i = result.size();
        if (i > 0 && result.get(i-1).header() && !showEmptyLists) {
            result.remove(i-1);
        }
        return result;
    }

    @NonNull
    public static String joinTasks(@Nullable Collection<Task> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        if (s==null) {
            return "";
        }
        Iterator<Task> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next().inFileFormat());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    @NonNull
    public static String join(@Nullable Collection<String> s, String delimiter) {
        if (s==null) {
            return "";
        }
        return Joiner.on(delimiter).join(s);
    }

    public static void setColor(@NonNull SpannableString ss, int color, String s) {
        ArrayList<String> strList = new ArrayList<>();
        strList.add(s);
        setColor(ss,color,strList);
    }

    public static void setColor(@NonNull SpannableString ss, int color ,  @NonNull List<String> items) {
        String data = ss.toString();
        for (String item : items) {
            int i = data.indexOf(item);
            if (i != -1) {
                ss.setSpan(new ForegroundColorSpan(color), i,
                        i + item.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static void setColor(@NonNull SpannableString ss, int color) {

        ss.setSpan(new ForegroundColorSpan(color), 0,
                ss.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @NotNull
    public static DateTime addInterval(@Nullable DateTime date, @NonNull String interval) {
        Pattern p = Pattern.compile("(\\d+)([dwmy])");
        Matcher m = p.matcher(interval.toLowerCase(Locale.getDefault()));
        int amount;
        String type;
        if (date == null) {
            date = DateTime.today(TimeZone.getDefault());
        }
        if(!m.find()) {
            //If the interval is invalid, just return the original date
            return date;
        }
        if(m.groupCount()==2) {
            amount = Integer.parseInt(m.group(1));
            type = m.group(2).toLowerCase(Locale.getDefault());
        } else {
            return date;
        }
        switch(type) {
            case "d":
                date = date.plusDays(amount);
                break;
            case "w":
                date = date.plusDays(7 * amount);
                break;
            case "m":
                date = date.plus(0, amount, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay);
                break;
            case "y":
                date = date.plus(amount, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay);
                break;
            default:
                // Dont add anything
                break;
        }
        return date;
    }


    @NonNull
    public static ArrayList<String> getCheckedItems(@NonNull ListView listView , boolean checked) {
        SparseBooleanArray checks = listView.getCheckedItemPositions();
        ArrayList<String> items = new ArrayList<>();
        for (int i = 0 ; i < checks.size() ; i++) {
            String item = (String)listView.getAdapter().getItem(checks.keyAt(i));
            if (checks.valueAt(i) && checked) {
                items.add(item);
            } else if (!checks.valueAt(i) && !checked) {
                items.add(item);
            }
        }
        return items;
    }

    @NonNull
    public static AlertDialog createDeferDialog(@NonNull final Activity act, int dateType, final boolean showNone,  @NonNull final InputDialogListener listener) {
        String[] keys = act.getResources().getStringArray(R.array.deferOptions);
        String today = "0d";
        String tomorrow = "1d";
        String oneWeek = "1w";
        String twoWeeks = "2w";
        String oneMonth = "1m";
        final String[] values  = { "", today, tomorrow, oneWeek, twoWeeks, oneMonth, "pick" };
        if (!showNone) {
            keys = Arrays.copyOfRange(keys, 1, keys.length);
        }
        int titleId;
        if (dateType==Task.DUE_DATE) {
            titleId = R.string.defer_due;
        } else {
            titleId = R.string.defer_threshold;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(titleId);
        builder.setItems(keys, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (!showNone) {
                    whichButton++;
                }
                String selected = values[whichButton];
                listener.onClick(selected);
            }
        });
        return builder.create();
    }


    public static void initGlobals(Globals globals, Task t) {
        globals.set("task", t.inFileFormat());

        if (t.getDueDate()!=null) {
            globals.set( "due", t.getDueDate().getMilliseconds(TimeZone.getDefault())/1000);
        } else {
            globals.set("due",LuaValue.NIL);
        }


        if (t.getThresholdDate()!=null) {
            globals.set("threshold", t.getThresholdDate().getMilliseconds(TimeZone.getDefault())/1000);
        } else {
            globals.set("threshold",LuaValue.NIL);
        }


        if (t.getCreateDate()!=null) {
            globals.set("createdate", new DateTime(t.getCreateDate()).getMilliseconds(TimeZone.getDefault())/1000);
        } else {
            globals.set("createdate",LuaValue.NIL);
        }


        if (t.getCompletionDate()!=null) {
            globals.set("completiondate", new DateTime(t.getCompletionDate()).getMilliseconds(TimeZone.getDefault())/1000);
        } else {
            globals.set("completiondate",LuaValue.NIL);
        }

        globals.set( "completed", LuaBoolean.valueOf(t.isCompleted()));
        globals.set( "priority", t.getPriority().getCode());

        globals.set("tags", javaListToLuaTable(t.getTags()));
        globals.set("lists", javaListToLuaTable(t.getLists()));
    }

    private static LuaValue javaListToLuaTable(List<String>javaList) {
        int size = javaList.size();
        if (size==0) return LuaValue.NIL;
        LuaString[] luaArray = new LuaString[javaList.size()];
        int i = 0;
        for (String item : javaList) {
            luaArray[i] = LuaString.valueOf(item);
            i++;
        }
        return LuaTable.listOf(luaArray);
    
    }

    public static void createCachedFile(Context context, String fileName,
            String content) throws IOException {

        File cacheFile = new File(context.getCacheDir() + File.separator
                + fileName);
        if (cacheFile.createNewFile()) {
            FileOutputStream fos = new FileOutputStream(cacheFile, false);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            PrintWriter pw = new PrintWriter(osw);
            pw.println(content);
            pw.flush();
            pw.close();
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        Logger log = LoggerFactory.getLogger(Util.class);
        if (destFile.createNewFile()) {
            log.debug("Destination file created {}" , destFile.getAbsolutePath());
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }

    public static void createCachedDatabase(Context context, File dbFile) throws IOException {
        File cacheFile = new File(context.getCacheDir() , dbFile.getName());
        copyFile(dbFile,cacheFile);
    }

    public static ArrayList<String> sortWithPrefix(List<String> items, boolean caseSensitive, String prefix) {
        ArrayList<String> result = new ArrayList<>();
        result.addAll(new AlphabeticalStringComparator(caseSensitive).sortedCopy(items));
        if (prefix !=null ) {
            result.add(0, prefix);
        }
        return result;
    }

    public static ArrayList<String> sortWithPrefix(Set<String> items, boolean caseSensitive, String prefix) {
        ArrayList<String> temp = new ArrayList<>();
        temp.addAll(items);
        return sortWithPrefix(temp, caseSensitive, prefix);
    }

    public static void shareText(Activity act, String text) {
        Logger log = LoggerFactory.getLogger(Util.class);
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "Simpletask list");

        // If text is small enough SEND it directly
        if (text.length() < 50000) {
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
        } else {

            // Create a cache file to pass in EXTRA_STREAM
            try {
                Util.createCachedFile(act,
                        Constants.SHARE_FILE_NAME, text);
                Uri fileUri = Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/"
                        + Constants.SHARE_FILE_NAME);
                shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
            } catch (Exception e) {
                log.warn("Failed to create file for sharing");
            }
        }
        act.startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    public static Dialog showLoadingOverlay(@NonNull Activity act, @Nullable Dialog visibleDialog, boolean show) {
        if (show) {
            Dialog newDialog = new Dialog(act);
            newDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            newDialog.setContentView(R.layout.loading);
            ProgressBar pr = (ProgressBar) newDialog.findViewById(R.id.progress);
            pr.getIndeterminateDrawable().setColorFilter(0xFF0099CC, android.graphics.PorterDuff.Mode.MULTIPLY);
            newDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            newDialog.setCancelable(false);
            newDialog.show();
            return newDialog;
        } else if (visibleDialog!=null && visibleDialog.isShowing()) {
            visibleDialog.dismiss();
        }
        return null;
    }
}
=======
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
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.util;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.ListView;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.SimpletaskException;
import nl.mpcjanssen.simpletask.sort.AlphabeticalStringComparator;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.token.Token;

public class Util {

    private static String TAG = Util.class.getSimpleName();

    private Util() {
    }

    public static String getTodayAsString() {
        return DateTime.today(TimeZone.getDefault()).format(Constants.DATE_FORMAT);
    }

    public static void showToastShort(@NotNull Context cxt, int resid) {
        Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show();
    }

    public static void showToastShort(@NotNull Context cxt, String msg) {
        Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show();
    }

    @Nullable
    public static ArrayList<String> tasksToString(@NotNull List<Task> tasks) {
        if (tasks==null) {
            return null;
        }
        ArrayList<String> result = new ArrayList<String>();
        for (Task t: tasks) {
            result.add(t.inFileFormat());
        }
        return result;
    }

    public interface InputDialogListener {
        void onClick(String input);
    }

    public static void createParentDirectory(@Nullable File dest) throws SimpletaskException {
        if (dest == null) {
            throw new SimpletaskException("createParentDirectory: dest is null");
        }
        File dir = dest.getParentFile();
        if (dir != null && !dir.exists()) {
            createParentDirectory(dir);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e(TAG, "Could not create dirs: " + dir.getAbsolutePath());
                    throw new SimpletaskException("Could not create dirs: "
                            + dir.getAbsolutePath());
                }
            }
        }
    }

    @NotNull
    public static String join(@Nullable Collection<?> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        if (s==null) {
        	return "";
        }
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    public static void setColor(@NotNull SpannableString ss, int color, String s) {
        ArrayList<String> strList = new ArrayList<String>();
        strList.add(s);
        setColor(ss,color,strList);
    }

    public static void setColor(@NotNull SpannableString ss, int color ,  @NotNull List<String> items) {
        String data = ss.toString();
        for (String item : items) {
            int i = data.indexOf(item);
            if (i != -1) {
                ss.setSpan(new ForegroundColorSpan(color), i,
                        i + item.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static void setColor(@NotNull SpannableString ss, int color) {

        ss.setSpan(new ForegroundColorSpan(color), 0,
                ss.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Nullable
    public static DateTime addInterval(@Nullable DateTime date, @NotNull String interval) {
        Pattern p = Pattern.compile("(\\d+)([dwmy])");
        Matcher m = p.matcher(interval.toLowerCase(Locale.getDefault()));
        int amount;
        String type;
        if (date == null) {
            date = DateTime.today(TimeZone.getDefault());
        }
        if(!m.find()) {
            return null;
        }
        if(m.groupCount()==2) {
            amount = Integer.parseInt(m.group(1));
            type = m.group(2).toLowerCase(Locale.getDefault());
        } else {
            return null;
        }
        if (type.equals("d")) {
            date = date.plusDays(amount);
        } else if (type.equals("w")) {
            date = date.plusDays(7 * amount);
        } else if (type.equals("m")) {
            date = date.plus(0, amount, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay);
        } else if (type.equals("y")) {
            date = date.plus(amount, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay);
        }
        return date;
    }

    @NotNull
    public static ArrayList<String> prefixItems(String prefix, @NotNull ArrayList<String> items) {
        ArrayList<String> result = new ArrayList<String>();
        for (String item : items) {
            result.add(prefix + item);
        }
        return result;
    }

    @NotNull
    public static ArrayList<String> getCheckedItems(@NotNull ListView listView , boolean checked) {
        SparseBooleanArray checks = listView.getCheckedItemPositions();
        ArrayList<String> items = new ArrayList<String>();
        for (int i = 0 ; i < checks.size() ; i++) {
            String item = (String)listView.getAdapter().getItem(checks.keyAt(i));
            if (checks.valueAt(i) && checked) {
                items.add(item);
            } else if (!checks.valueAt(i) && !checked) {
                items.add(item);
            }
        }
        return items;
    }

    @NotNull
    public static AlertDialog createDeferDialog(@NotNull final Activity act, int dateType, final boolean showNone,  @NotNull final InputDialogListener listener) {
        String[] keys = act.getResources().getStringArray(R.array.deferOptions);
        String today = "0d";
        String tomorrow = "1d";
        String oneWeek = "1w";
        String twoWeeks = "2w";
        String oneMonth = "1m";
        final String[] values  = { "", today, tomorrow, oneWeek, twoWeeks, oneMonth, "pick" };
        if (!showNone) {
            keys = Arrays.copyOfRange(keys, 1, keys.length);
        }
        int titleId;
        if (dateType==Task.DUE_DATE) {
            titleId = R.string.defer_due;
        } else {
            titleId = R.string.defer_threshold;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(titleId);
        builder.setItems(keys,  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (!showNone) {
                    whichButton++;
                }
                String selected = values[whichButton];
                listener.onClick(selected);
            }
        });
        return builder.create();
    }

    public static void fillScope(ScriptableObject scope, Task t) {
        scope.defineProperty("task", t.inFileFormat(), 0);
        if (t.getDueDate()!=null) {
            scope.defineProperty("due", t.getDueDate().getMilliseconds(TimeZone.getDefault()), 0);
        } else {
            scope.defineProperty("due", t.getDueDate(), 0);
        }
        if (t.getThresholdDate()!=null) {
            scope.defineProperty("threshold", t.getThresholdDate().getMilliseconds(TimeZone.getDefault()), 0);
        } else {
            scope.defineProperty("threshold",t.getThresholdDate(), 0);
        }
        if (t.getCreateDate()!=null) {
            scope.defineProperty("createdate", new DateTime(t.getCreateDate()).getMilliseconds(TimeZone.getDefault()), 0);
        } else {
            scope.defineProperty("createdate",t.getCreateDate(), 0);
        }
        if (t.getCompletionDate()!=null) {
            scope.defineProperty("completiondate", new DateTime(t.getCompletionDate()).getMilliseconds(TimeZone.getDefault()), 0);
        } else {
            scope.defineProperty("completiondate",t.getCompletionDate(), 0);
        }
        scope.defineProperty("completed", t.isCompleted(), 0);
        scope.defineProperty("priority", t.getPriority().getCode(),0);
        scope.defineProperty("recurrence", t.getRecurrencePattern(), 0);
        scope.defineProperty("tags", org.mozilla.javascript.Context.javaToJS(t.getTags(), scope), 0);
        scope.defineProperty("lists", org.mozilla.javascript.Context.javaToJS(t.getLists(), scope), 0);
        scope.defineProperty("tokens", org.mozilla.javascript.Context.javaToJS(t.getTokens(), scope), 0);
        // Variable for static fields from Token class
        Token tempToken = new Token(0,"") {
        };
        scope.defineProperty("Token", org.mozilla.javascript.Context.javaToJS(tempToken, scope), 0);
        scope.defineProperty("taskObj", org.mozilla.javascript.Context.javaToJS(t, scope), 0);
    }

    public static void createCachedFile(Context context, String fileName,
            String content) throws IOException {
 
        File cacheFile = new File(context.getCacheDir() + File.separator
                + fileName);
        cacheFile.createNewFile();
 
        FileOutputStream fos = new FileOutputStream(cacheFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
        PrintWriter pw = new PrintWriter(osw);
 
        pw.println(content);
 
        pw.flush();
        pw.close();
    }

    public static ArrayList<String> sortWithPrefix(List<String> items, boolean caseSensitive, String prefix) {
        ArrayList<String> result = new ArrayList<String>();
        result.addAll(new AlphabeticalStringComparator(caseSensitive).sortedCopy(items));
        if (prefix !=null ) {
            result.add(0,prefix);
        }
        return result;
    }

    public static ArrayList<String> sortWithPrefix(Set<String> items, boolean caseSensitive, String prefix) {
        ArrayList<String> temp = new ArrayList<String>();
        temp.addAll(items);
        return sortWithPrefix(temp, caseSensitive, prefix);
    }
}
>>>>>>> origin/macroid
=======
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
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.util;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
<<<<<<< HEAD
=======
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
>>>>>>> origin/extsdwrite
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.ListView;
import android.widget.Toast;
import com.google.common.base.Joiner;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.SimpletaskException;
import nl.mpcjanssen.simpletask.sort.AlphabeticalStringComparator;
import nl.mpcjanssen.simpletask.task.Task;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclBoolean;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclObject;
import tcl.lang.TclString;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private static String TAG = Util.class.getSimpleName();

    private Util() {
    }

    public static String getTodayAsString() {
        return DateTime.today(TimeZone.getDefault()).format(Constants.DATE_FORMAT);
    }

    public static void showToastShort(@NotNull Context cxt, int resid) {
        Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show();
    }

    public static void showToastShort(@NotNull Context cxt, String msg) {
        Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show();
    }

    @Nullable
    public static ArrayList<String> tasksToString(@NotNull List<Task> tasks) {
        ArrayList<String> result = new ArrayList<>();
        for (Task t: tasks) {
            result.add(t.inFileFormat());
        }
        return result;
    }

    public interface InputDialogListener {
        void onClick(String input);
    }

<<<<<<< HEAD
    public static void createParentDirectory(@Nullable File dest) throws SimpletaskException {
=======
    public static void createParentDirectory(@Nullable DocumentFile dest) throws TodoException {
        Logger log = LoggerFactory.getLogger(Util.class);
>>>>>>> origin/extsdwrite
        if (dest == null) {
            throw new SimpletaskException("createParentDirectory: dest is null");
        }
        DocumentFile dir = dest.getParentFile();
        if (dir != null && !dir.exists()) {
            createParentDirectory(dir);
<<<<<<< HEAD
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e(TAG, "Could not create dirs: " + dir.getAbsolutePath());
                    throw new SimpletaskException("Could not create dirs: "
                            + dir.getAbsolutePath());
                }
            }
=======
            dir.getParentFile().createDirectory(dir.getName());
>>>>>>> origin/extsdwrite
        }
    }

<<<<<<< HEAD
    @NotNull
=======
    public static List<VisibleLine> addHeaderLines(List<Task> visibleTasks, String firstSort, String no_header, boolean showHidden, boolean showEmptyLists) {
        String header = "" ;
        String newHeader;
        ArrayList<VisibleLine> result = new ArrayList<>();
        for (Task t : visibleTasks) {
            newHeader = t.getHeader(firstSort, no_header);
            if (!header.equals(newHeader)) {
                VisibleLine headerLine = new VisibleLine(newHeader);
                int last = result.size() - 1;
                if (last != -1 && result.get(last).header && !showEmptyLists) {
                    // replace empty preceding header
                    result.set(last, headerLine);
                } else {
                    result.add(headerLine);
                }
                header = newHeader;
            }

            if (t.isVisible() || showHidden) {
                // enduring tasks should not be displayed
                VisibleLine taskLine = new VisibleLine(t);
                result.add(taskLine);
            }
        }

        // Clean up possible last empty list header that should be hidden
        int i = result.size();
        if (i > 0 && result.get(i-1).header && !showEmptyLists) {
            result.remove(i - 1);
        }
        return result;
    }

    @NonNull
>>>>>>> origin/tclscript
    public static String joinTasks(@Nullable Collection<Task> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        if (s==null) {
            return "";
        }
        Iterator<Task> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next().inFileFormat());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    @NotNull
    public static String join(@Nullable Collection<String> s, String delimiter) {
        if (s==null) {
            return "";
        }
        return Joiner.on(delimiter).join(s);
    }

    public static void setColor(@NotNull SpannableString ss, int color, String s) {
        ArrayList<String> strList = new ArrayList<>();
        strList.add(s);
        setColor(ss,color,strList);
    }

    public static void setColor(@NotNull SpannableString ss, int color ,  @NotNull List<String> items) {
        String data = ss.toString();
        for (String item : items) {
            int i = data.indexOf(item);
            if (i != -1) {
                ss.setSpan(new ForegroundColorSpan(color), i,
                        i + item.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static void setColor(@NotNull SpannableString ss, int color) {

        ss.setSpan(new ForegroundColorSpan(color), 0,
                ss.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Nullable
    public static DateTime addInterval(@Nullable DateTime date, @NotNull String interval) {
        Pattern p = Pattern.compile("(\\d+)([dwmy])");
        Matcher m = p.matcher(interval.toLowerCase(Locale.getDefault()));
        int amount;
        String type;
        if (date == null) {
            date = DateTime.today(TimeZone.getDefault());
        }
        if(!m.find()) {
            return null;
        }
        if(m.groupCount()==2) {
            amount = Integer.parseInt(m.group(1));
            type = m.group(2).toLowerCase(Locale.getDefault());
        } else {
            return null;
        }
        switch(type) {
            case "d":
                date = date.plusDays(amount);
                break;
            case "w":
                date = date.plusDays(7 * amount);
                break;
            case "m":
                date = date.plus(0, amount, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay);
                break;
            case "y":
                date = date.plus(amount, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay);
                break;
            default:
                // Dont add anything
                break;
        }
        return date;
    }

    @NotNull
    public static ArrayList<String> prefixItems(String prefix, @NotNull ArrayList<String> items) {
        ArrayList<String> result = new ArrayList<>();
        for (String item : items) {
            result.add(prefix + item);
        }
        return result;
    }

    @NotNull
    public static ArrayList<String> getCheckedItems(@NotNull ListView listView , boolean checked) {
        SparseBooleanArray checks = listView.getCheckedItemPositions();
        ArrayList<String> items = new ArrayList<>();
        for (int i = 0 ; i < checks.size() ; i++) {
            String item = (String)listView.getAdapter().getItem(checks.keyAt(i));
            if (checks.valueAt(i) && checked) {
                items.add(item);
            } else if (!checks.valueAt(i) && !checked) {
                items.add(item);
            }
        }
        return items;
    }

    @NotNull
    public static AlertDialog createDeferDialog(@NotNull final Activity act, int dateType, final boolean showNone,  @NotNull final InputDialogListener listener) {
        String[] keys = act.getResources().getStringArray(R.array.deferOptions);
        String today = "0d";
        String tomorrow = "1d";
        String oneWeek = "1w";
        String twoWeeks = "2w";
        String oneMonth = "1m";
        final String[] values  = { "", today, tomorrow, oneWeek, twoWeeks, oneMonth, "pick" };
        if (!showNone) {
            keys = Arrays.copyOfRange(keys, 1, keys.length);
        }
        int titleId;
        if (dateType==Task.DUE_DATE) {
            titleId = R.string.defer_due;
        } else {
            titleId = R.string.defer_threshold;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(titleId);
        builder.setItems(keys,  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (!showNone) {
                    whichButton++;
                }
                String selected = values[whichButton];
                listener.onClick(selected);
            }
        });
        return builder.create();
    }



    public static void initGlobals(Interp interp, Task t) throws TclException {
        interp.setVar("task", null, t.inFileFormat(), TCL.GLOBAL_ONLY);

        if (t.getDueDate()!=null) {
            interp.setVar("due", null, t.getDueDate().getMilliseconds(TimeZone.getDefault()) / 1000, TCL.GLOBAL_ONLY);
        } else {
            interp.setVar("due", null,"", TCL.GLOBAL_ONLY);
        }


        if (t.getThresholdDate()!=null) {
            interp.setVar("threshold", null, t.getThresholdDate().getMilliseconds(TimeZone.getDefault()) / 1000, TCL.GLOBAL_ONLY);
        } else {
            interp.setVar("threshold", null, "", TCL.GLOBAL_ONLY);;
        }


        if (t.getCreateDate()!=null) {
            interp.setVar("createdate", null, new DateTime(t.getCreateDate()).getMilliseconds(TimeZone.getDefault()) / 1000, TCL.GLOBAL_ONLY);
        } else {
            interp.setVar("createdate", null, "", TCL.GLOBAL_ONLY);
        }


        if (t.getCompletionDate()!=null) {
            interp.setVar("completiondate", null, new DateTime(t.getCompletionDate()).getMilliseconds(TimeZone.getDefault()) / 1000, TCL.GLOBAL_ONLY);
        } else {
            interp.setVar("completiondate", null, "", TCL.GLOBAL_ONLY);
        }

        interp.setVar("completed", null, TclBoolean.newInstance(t.isCompleted()), TCL.GLOBAL_ONLY);
        interp.setVar("priority", null, t.getPriority().getCode(), TCL.GLOBAL_ONLY);

<<<<<<< HEAD
        globals.set("tags",javaListToLuaTable(t.getTags()));
        globals.set("lists",javaListToLuaTable(t.getLists()));
=======
        interp.setVar("tags", null,  javaListToTclList(interp, t.getTags()), TCL.GLOBAL_ONLY);
        interp.setVar("lists", null,  javaListToTclList(interp, t.getLists()), TCL.GLOBAL_ONLY);
>>>>>>> origin/tclscript
    }

    private static LuaValue javaListToLuaTable(List<String>javaList) {
        int size = javaList.size();
        if (size==0) return LuaValue.NIL;
        LuaString[] luaArray = new LuaString[javaList.size()];
        int i = 0;
        for (String item : javaList) {
            luaArray[i] = LuaString.valueOf(item);
            i++;
        }
        return LuaTable.listOf(luaArray);
    
    }

    private static TclObject javaListToTclList(Interp interp, List<String>javaList) throws TclException {
        TclObject result = TclList.newInstance();
        for (String item : javaList) {
            TclList.append(interp, result, TclString.newInstance(item));
        }
        return result;
    }

    public static void createCachedFile(Context context, String fileName,
            String content) throws IOException {

        File cacheFile = new File(context.getCacheDir() + File.separator
                + fileName);
        if (cacheFile.createNewFile()) {
            FileOutputStream fos = new FileOutputStream(cacheFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            PrintWriter pw = new PrintWriter(osw);
            pw.println(content);
            pw.flush();
            pw.close();
        }
    }

    public static ArrayList<String> sortWithPrefix(List<String> items, boolean caseSensitive, String prefix) {
        ArrayList<String> result = new ArrayList<>();
        result.addAll(new AlphabeticalStringComparator(caseSensitive).sortedCopy(items));
        if (prefix !=null ) {
            result.add(0,prefix);
        }
        return result;
    }

    public static ArrayList<String> sortWithPrefix(Set<String> items, boolean caseSensitive, String prefix) {
        ArrayList<String> temp = new ArrayList<>();
        temp.addAll(items);
        return sortWithPrefix(temp, caseSensitive, prefix);
    }
<<<<<<< HEAD
=======

    public static void shareText(Activity act, String text) {
        Logger log = LoggerFactory.getLogger(Util.class);
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "Simpletask list");

        // If text is small enough SEND it directly
        if (text.length() < 50000) {
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
        } else {

            // Create a cache file to pass in EXTRA_STREAM
            try {
                Util.createCachedFile(act,
                        Constants.SHARE_FILE_NAME, text);
                Uri fileUri = Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/"
                        + Constants.SHARE_FILE_NAME);
                shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
            } catch (Exception e) {
                log.warn("Failed to create file for sharing");
            }
        }
        act.startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    public static Dialog showLoadingOverlay(@NonNull Activity act, @Nullable Dialog visibleDialog, boolean show) {
        if (show) {
            Dialog newDialog = new Dialog(act);
            newDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            newDialog.setContentView(R.layout.loading);
            ProgressBar pr = (ProgressBar) newDialog.findViewById(R.id.progress);
            pr.getIndeterminateDrawable().setColorFilter(0xFF0099CC, android.graphics.PorterDuff.Mode.MULTIPLY);
            newDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            newDialog.setCancelable(false);
            newDialog.show();
            return newDialog;
        } else if (visibleDialog!=null && visibleDialog.isShowing()) {
            visibleDialog.dismiss();
        }
        return null;
    }

    public static void showConfirmationDialog(@NonNull Context cxt, boolean show, int msgid, int titleid,
                                              @NonNull DialogInterface.OnClickListener oklistener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        builder.setTitle(titleid);
        builder.setMessage(msgid);
        builder.setPositiveButton(android.R.string.ok, oklistener);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setCancelable(true);
        Dialog dialog = builder.create();
        dialog.show();
        if (show) {
            dialog.show();
        } else {
            oklistener.onClick(dialog , DialogInterface.BUTTON_POSITIVE);
        }
    }
>>>>>>> origin/extsdwrite
}
>>>>>>> origin/dropbox-change
