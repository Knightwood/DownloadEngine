package com.kiylx.download_module.view

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport


class DownloadInfoSources() {
    val ob: PropertyChangeSupport = PropertyChangeSupport(this)
    var infoList: MutableList<SimpleDownloadInfo> = mutableListOf()

    fun addInfo(info: SimpleDownloadInfo) {
        infoList.add(info);
        ob.firePropertyChange(PropertyChangeEvent(info,"info",null,info))
    }

    /**
     * 在事件源上添加监听，实际上是在PropertyChangeSupport对象上添加监听
     */
    fun addListener(listener: PropertyChangeListener?) {
        ob.addPropertyChangeListener(listener)
    }

    /**
     * 同上
     */
    fun removeListener(listener: PropertyChangeListener?) {
        ob.removePropertyChangeListener(listener)
    }

    fun syncInfo() {

    }
}