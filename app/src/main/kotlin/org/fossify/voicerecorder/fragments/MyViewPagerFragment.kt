package org.fossify.voicerecorder.fragments

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.voicerecorder.extensions.getAllRecordings
import org.fossify.voicerecorder.models.Recording

abstract class MyViewPagerFragment(context: Context, attributeSet: AttributeSet) :
    ConstraintLayout(context, attributeSet) {
    abstract fun onResume()

    abstract fun onDestroy()

    open fun onLoadingStart() {}

    open fun onLoadingEnd(recordings: ArrayList<Recording>) {}

    open fun loadRecordings(trashed: Boolean = false) {
        onLoadingStart()
        ensureBackgroundThread {
            val recordings = context.getAllRecordings(trashed)
                .apply { sortByDescending { it.timestamp } }

            (context as? Activity)?.runOnUiThread {
                onLoadingEnd(recordings)
            }
        }
    }
}
