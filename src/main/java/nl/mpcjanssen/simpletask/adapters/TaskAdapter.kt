package nl.mpcjanssen.simpletask.adapters

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ListAdapter
import android.widget.TextView
import com.buildware.widget.indeterm.IndeterminateCheckBox
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.task.Priority
import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoListItem
import nl.mpcjanssen.simpletask.util.*
import tcl.lang.TCL
import tcl.lang.TclString
import java.util.*


class TaskAdapter(private val m_app: TodoApplication,  val act: Simpletask) : RecyclerView.Adapter<TaskAdapter.ViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder? {
        // create a new view
        val v  = when (viewType) {
            0 -> LayoutInflater.from(parent?.context).inflate(R.layout.list_header, parent, false)
            2 -> LayoutInflater.from(parent?.context).inflate(R.layout.empty_list_item, parent, false)
            else-> LayoutInflater.from(parent?.context).inflate(R.layout.list_item, parent, false)
        // set the view's size, margins, paddings and layout parameters
        }
        val vh = TaskAdapter.ViewHolder(v)
        return vh
    }

    override fun getItemCount(): Int {
        return visibleLines.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == visibleLines.size) {
            return 2
        }
        val line = visibleLines[position]
        if (line.header) {
            return 0
        } else {
            return 1
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // each data item is just a string in this case

        var taskText: TextView?
        var headerTitle: TextView?
        var taskAge: TextView?
        var taskDue: TextView?
        var taskThreshold: TextView?
        var cbCompleted: CheckBox?
        var taskBar: View?

        init {
            taskText = view.findViewById(R.id.tasktext) as TextView?
            taskAge = view.findViewById(R.id.taskage) as TextView?
            taskDue = view.findViewById(R.id.taskdue) as TextView?
            taskThreshold = view.findViewById(R.id.taskthreshold) as TextView?
            taskBar = view.findViewById(nl.mpcjanssen.simpletask.R.id.datebar)
            
            cbCompleted = view.findViewById(R.id.checkBox) as CheckBox?
            headerTitle = view.findViewById(nl.mpcjanssen.simpletask.R.id.list_header_title) as TextView?

        }
    }

    internal var visibleLines = ArrayList<VisibleLine>()

    private var displayProcAvailable = false

    private var mFilter: ActiveFilter? = null

    internal fun setFilteredTasks(filter: ActiveFilter?) {
        mFilter = filter
        val actTitle = if (m_app.showTodoPath()) {
            m_app.todoFileName.replace("([^/])[^/]*/".toRegex(), "$1/")
        } else {
            m_app.getString(nl.mpcjanssen.simpletask.R.string.app_label)
        }
        act.title = actTitle
        if (!mFilter?.script.isNullOrEmpty()) {
            try {
                val scriptObj = TclString.newInstance(mFilter?.script)
                act.interp.eval(scriptObj, TCL.EVAL_GLOBAL)
                displayProcAvailable = act.interp.getCommand(Constants.TCL_DISPLAY_COMMAND) != null
                if (!displayProcAvailable) {
                    log.info(ActiveFilter.TAG, "Tcl script doesn't define a ${Constants.TCL_DISPLAY_COMMAND} command")
                }
            } catch (e: Exception) {
                log.error(ActiveFilter.TAG, "Error in Tcl script: ${e.cause} -> ${act.interp.getResult()}")
            }
        }


        act.updateConnectivityIndicator()
        val visibleTasks: List<TodoListItem>
        log.info("TaskAdapter", "setFilteredTasks called: " + act.todoList)
        val activeFilter = mFilter ?: return
        val sorts = activeFilter.getSort(m_app.defaultSorts)
        visibleTasks = act.todoList.getSortedTasksCopy(activeFilter, sorts, m_app.sortCaseSensitive())
        visibleLines.clear()


        var firstGroupSortIndex = 0
        if (sorts.size > 1 && sorts[0].contains("completed") || sorts[0].contains("future")) {
            firstGroupSortIndex++
            if (sorts.size > 2 && sorts[1].contains("completed") || sorts[1].contains("future")) {
                firstGroupSortIndex++
            }
        }


        val firstSort = sorts[firstGroupSortIndex]
        visibleLines.addAll(addHeaderLines(visibleTasks, firstSort, act.getString(nl.mpcjanssen.simpletask.R.string.no_header)))
        notifyDataSetChanged()
        act.updateFilterBar()
    }

    val countVisibleTodoItems: Int
        get() {
            var count = 0
            for (line in visibleLines) {
                if (!line.header) {
                    count++
                }
            }
            return count
        }

    /*
    ** Get the adapter position for task
    */
    fun getPosition(task: TodoListItem): Int {
        val line = TaskLine(task)
        return visibleLines.indexOf(line)
    }

    fun getItemAt (position: Int) : TodoListItem? {
        return visibleLines.getOrNull(position)?.task
    }
    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        if (position == visibleLines.size) {
            return
        }
        val line = visibleLines[position]
        if (line.header) {
            val t = holder?.headerTitle
            t?.text = line.title
            t?.textSize = m_app.activeFontSize

        } else {

            val item = line.task ?: return
            val task = item.task

            if (m_app.showCompleteCheckbox()) {
                holder?.cbCompleted?.visibility = View.VISIBLE
            } else {
                holder?.cbCompleted?.visibility = View.GONE
            }
            if (!m_app.hasExtendedTaskView()) {
                holder?.taskBar?.visibility = View.GONE
            }


            // Also massage with Tcl
            val txt = if (displayProcAvailable) {
                try {
                    // Call the filter proc
                    act.interp.eval(buildDisplayTclCommand(act.interp, task), TCL.EVAL_GLOBAL)
                    act.interp.result.toString()
                } catch (e: Exception) {
                    act.interp.result.toString()
                }
            } else {
                var tokensToShow = TToken.ALL
                // Hide dates if we have a date bar
                if (m_app.hasExtendedTaskView()) {
                    tokensToShow = tokensToShow and TToken.COMPLETED_DATE.inv()
                    tokensToShow = tokensToShow and TToken.THRESHOLD_DATE.inv()
                    tokensToShow = tokensToShow and TToken.DUE_DATE.inv()
                }
                tokensToShow = tokensToShow and TToken.CREATION_DATE.inv()
                tokensToShow = tokensToShow and TToken.COMPLETED.inv()

                if (mFilter!!.hideLists) {
                    tokensToShow = tokensToShow and TToken.LIST.inv()
                }
                if (mFilter!!.hideTags) {
                    tokensToShow = tokensToShow and TToken.TTAG.inv()
                }
                task.showParts(tokensToShow)
            }




            val ss = SpannableString(txt)

            val colorizeStrings = ArrayList<String>()
            val contexts = task.lists
            for (context in contexts) {
                colorizeStrings.add("@" + context)
            }
            setColor(ss, Color.GRAY, colorizeStrings)
            colorizeStrings.clear()
            val projects = task.tags
            for (project in projects) {
                colorizeStrings.add("+" + project)
            }
            setColor(ss, Color.GRAY, colorizeStrings)

            val priorityColor: Int
            val priority = task.priority
            when (priority) {
                Priority.A -> priorityColor = ContextCompat.getColor(m_app, android.R.color.holo_red_dark)
                Priority.B -> priorityColor = ContextCompat.getColor(m_app, android.R.color.holo_orange_dark)
                Priority.C -> priorityColor = ContextCompat.getColor(m_app, android.R.color.holo_green_dark)
                Priority.D -> priorityColor = ContextCompat.getColor(m_app, android.R.color.holo_blue_dark)
                else -> priorityColor = ContextCompat.getColor(m_app, android.R.color.darker_gray)
            }
            setColor(ss, priorityColor, priority.inFileFormat())
            val completed = task.isCompleted()
            val taskText = holder?.taskText!!
            val taskAge = holder?.taskAge!!
            val taskDue = holder?.taskDue!!
            val taskThreshold = holder?.taskThreshold!!

            taskAge.textSize =  m_app.activeFontSize *  m_app.dateBarRelativeSize
            taskDue.textSize = m_app.activeFontSize * m_app.dateBarRelativeSize
            taskThreshold.textSize = m_app.activeFontSize * m_app.dateBarRelativeSize

            val cb = holder?.cbCompleted!!
            taskText.text = ss

            handleEllipsis(holder?.taskText as TextView)


            if (completed) {
                // log.info( "Striking through " + task.getText());
                taskText.paintFlags = taskText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holder?.taskAge!!.paintFlags = taskAge.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                cb.isChecked = true
                cb.setOnClickListener({
                    undoCompleteTasks(item)
                    act.closeSelectionMode()
                    act.todoList.notifyChanged(m_app.fileStore, m_app.todoFileName, m_app.eol, m_app, true)
                })
            } else {
                taskText.paintFlags = taskText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                taskAge.paintFlags = taskAge.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                cb.isChecked = false

                cb.setOnClickListener {
                    completeTasks(item)
                    act.closeSelectionMode()
                    act.todoList.notifyChanged(m_app.fileStore, m_app.todoFileName, m_app.eol, m_app, true)
                }

            }

            val relAge = getRelativeAge(task, act)
            val relDue = getRelativeDueDate(task, m_app, ContextCompat.getColor(m_app, android.R.color.holo_green_light),
                    ContextCompat.getColor(m_app, android.R.color.holo_red_light),
                    m_app.hasColorDueDates())
            val relativeThresholdDate = getRelativeThresholdDate(task, act)
            if (!isEmptyOrNull(relAge) && !mFilter!!.hideCreateDate) {
                taskAge.text = relAge
                taskAge.visibility = View.VISIBLE
            } else {
                taskAge.text = ""
                taskAge.visibility = View.GONE
            }

            if (relDue != null) {
                taskDue.text = relDue
                taskDue.visibility = View.VISIBLE
            } else {
                taskDue.text = ""
                taskDue.visibility = View.GONE
            }
            if (!isEmptyOrNull(relativeThresholdDate)) {
                taskThreshold.text = relativeThresholdDate
                taskThreshold.visibility = View.VISIBLE
            } else {
                taskThreshold.text = ""
                taskThreshold.visibility = View.GONE
            }
        }
    }

    private fun undoCompleteTasks(task: TodoListItem?) {
        if (task == null) return
        val tasks = ArrayList<TodoListItem>()
        tasks.add(task)
        act.undoCompleteTasks(tasks)
    }


    private fun completeTasks(task: TodoListItem) {
        val tasks = ArrayList<TodoListItem>()
        tasks.add(task)
        act.completeTasks(tasks)
    }

    private fun handleEllipsis(taskText: TextView) {
        val noEllipsizeValue = "no_ellipsize"
        val ellipsizeKey = m_app.getString(R.string.task_text_ellipsizing_pref_key)
        val ellipsizePref = m_app.prefs.getString(ellipsizeKey, noEllipsizeValue)

        if (noEllipsizeValue != ellipsizePref) {
            val truncateAt: TextUtils.TruncateAt?
            when (ellipsizePref) {
                "start" -> truncateAt = TextUtils.TruncateAt.START
                "end" -> truncateAt = TextUtils.TruncateAt.END
                "middle" -> truncateAt = TextUtils.TruncateAt.MIDDLE
                "marquee" -> truncateAt = TextUtils.TruncateAt.MARQUEE
                else -> truncateAt = null
            }

            if (truncateAt != null) {
                taskText.maxLines = 1
                taskText.setHorizontallyScrolling(true)
                taskText.ellipsize = truncateAt
            } else {
                log.warn("TaskAdapter", "Unrecognized preference value for task text ellipsis: {} !" + ellipsizePref)
            }
        }
    }
    
}