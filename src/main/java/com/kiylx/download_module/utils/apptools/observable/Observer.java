package com.kiylx.download_module.utils.apptools.observable;

/**
 * 创建者 kiylx
 * 创建时间 2020/9/8 17:00
 * packageName：com.example.kiylx.ti.tool.observable
 * 描述：
 */
public interface Observer {
    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param   o     the observable object.
     * @param   arg   an argument passed to the <code>notifyObservers</code>
     *                 method.
     */
    void update(Observable o, Object... arg);
}
