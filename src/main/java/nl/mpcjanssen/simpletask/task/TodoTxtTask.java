/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 * <p/>
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 * <p/>
 * LICENSE:
 * <p/>
 * Simpletask is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
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
 * @author Mark Janssen, Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2012- Mark Janssen
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.task;

import nl.mpcjanssen.simpletask.dao.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TodoTxtTask {
    public static String TAG = TodoTxtTask.class.getName();
    public final static int DUE_DATE = 0;
    public final static int THRESHOLD_DATE = 1;
    private static final long serialVersionUID = 1L;
    private final static Pattern LIST_PATTERN = Pattern
            .compile("^@(\\S*\\w)(.*)");
    private final static Pattern TAG_PATTERN = Pattern
            .compile("^\\+(\\S*\\w)(.*)");
    private static final Pattern HIDDEN_PATTERN = Pattern
            .compile("^[Hh]:([01])(.*)");
    private static final Pattern DUE_PATTERN = Pattern
            .compile("^[Dd][Uu][Ee]:(\\d{4}-\\d{2}-\\d{2})(.*)");
    private static final Pattern THRESHOLD_PATTERN = Pattern
            .compile("^[Tt]:(\\d{4}-\\d{2}-\\d{2})(.*)");
    private static final Pattern RECURRENCE_PATTERN = Pattern
            .compile("(^||\\s)[Rr][Ee][Cc]:((\\+?)\\d+[dDwWmMyY])");
    private final static Pattern PRIORITY_PATTERN = Pattern
            .compile("^(\\(([A-Z])\\) )(.*)");
    private final static Pattern SINGLE_DATE_PATTERN = Pattern
            .compile("^(\\d{4}-\\d{2}-\\d{2} )(.*)");
    private final static String COMPLETED_PREFIX = "x ";

    public static void addToDatabase(Daos daos , int line, String text) {
        EntryDao entryDao = daos.getEntryDao();
        EntryListDao  listDao = daos.getListDao();
        EntryTagDao tagDao  = daos.getTagDao();

        Entry entry = new Entry();
        entry.setLine(line);
        entry.setSelected(false);
        entry.setText(text);
        Matcher m;
        String remaining = text;
        if (remaining.startsWith(COMPLETED_PREFIX)) {
            entry.setCompleted(true);
            remaining = text.substring(2);
            m = SINGLE_DATE_PATTERN.matcher(remaining);
            // read optional completion date (this 'violates' the format spec)
            // be liberal with date format errors
            if (m.matches()) {
                entry.setCompletionDate(m.group(1).trim());
                remaining = m.group(2);
                m = SINGLE_DATE_PATTERN.matcher(remaining);
                // read possible create date
                if (m.matches()) {
                    entry.setCreateDate(m.group(1).trim());
                    remaining = m.group(2);
                }
            }
        } else {
            entry.setCompleted(false);
        }

        // Check for optional priority
        m = PRIORITY_PATTERN.matcher(remaining);
        if (m.matches()) {
            entry.setPriority(Priority.toPriority(m.group(2)).getCode());
            remaining = m.group(3);
        } else {
            entry.setPriority(Priority.NONE.getCode());
        }
        // Check for optional creation date
        m = SINGLE_DATE_PATTERN.matcher(remaining);
        if (m.matches()) {
            entry.setCreateDate(m.group(1).trim());
            remaining = m.group(2);
        }
        entry.setEndOfCompPrefix(text.length()- remaining.length());

        while (remaining.length() > 0) {
            if (remaining.startsWith(" ")) {
                String leading = "";
                while (remaining.length() > 0 && remaining.startsWith(" ")) {
                    leading = leading + " ";
                    remaining = remaining.substring(1);
                }
                continue;
            }
            m = LIST_PATTERN.matcher(remaining);
            if (m.matches()) {
                EntryList entryList = new EntryList();
                entryList.setEntryLine(entry.getLine());
                entryList.setText(m.group(1));
                remaining = m.group(2);
                listDao.insert(entryList);
                continue;
            }
            m = TAG_PATTERN.matcher(remaining);
            if (m.matches()) {
                EntryTag entryTag = new EntryTag();
                entryTag.setEntryLine(entry.getLine());
                entryTag.setText(m.group(1));
                remaining = m.group(2);
                tagDao.insert(entryTag);
                continue;
            }
            m = THRESHOLD_PATTERN.matcher(remaining);
            if (m.matches()) {
                entry.setThresholdDate(m.group(1));
                remaining = m.group(2);
                continue;
            }
            m = DUE_PATTERN.matcher(remaining);
            if (m.matches()) {
                entry.setDueDate(m.group(1));
                remaining = m.group(2);
                continue;
            }
            m = HIDDEN_PATTERN.matcher(remaining);
            if (m.matches()) {
                String match = m.group(1);
                remaining = m.group(2);
                entry.setHidden(match.equals("1"));
                continue;
            } else {
                entry.setHidden(false);
            }
            String leading = "";
            while (remaining.length() > 0 && !remaining.startsWith(" ")) {
                leading = leading + remaining.substring(0, 1);
                remaining = remaining.substring(1);
            }

        }
        entryDao.insert(entry);
    }

}
