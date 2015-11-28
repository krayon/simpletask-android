import android.Keys._

android.Plugin.androidBuild
scalaVersion := "2.11.7"

platformTarget in Android := "android-23"

libraryDependencies +=  "com.android.support" % "support-v4" % "22.2.0" 
libraryDependencies +=  "com.android.support" % "appcompat-v7" % "22.2.0" 
libraryDependencies +=  "com.android.support" % "design" % "22.2.0" 
