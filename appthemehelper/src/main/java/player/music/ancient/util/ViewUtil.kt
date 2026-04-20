package player.music.ancient.util

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewTreeObserver

object ViewUtil {
    @JvmStatic
    fun removeOnGlobalLayoutListener(v: View, listener: ViewTreeObserver.OnGlobalLayoutListener) {
        v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }

    @JvmStatic
    fun setBackgroundCompat(view: View, drawable: Drawable?) {
        view.background = drawable
    }
}
