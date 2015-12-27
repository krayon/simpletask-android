package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.Task

trait VisibleLine {
  def header: Boolean
  def title: String
  def task: Task
}

case class HeaderLine (title: String) extends VisibleLine {
  def header = true
  def task = null
}

case class TaskLine (task: Task) extends VisibleLine {
  def header = false
  def title = null
}