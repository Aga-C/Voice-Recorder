package org.fossify.voicerecorder.extensions

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.fossify.commons.extensions.getDocumentSdk30
import org.fossify.commons.extensions.getDuration
import org.fossify.commons.extensions.internalStoragePath
import org.fossify.commons.extensions.isAudioFast
import org.fossify.commons.helpers.isRPlus
import org.fossify.voicerecorder.helpers.Config
import org.fossify.voicerecorder.helpers.DEFAULT_RECORDINGS_FOLDER
import org.fossify.voicerecorder.helpers.IS_RECORDING
import org.fossify.voicerecorder.helpers.MyWidgetRecordDisplayProvider
import org.fossify.voicerecorder.helpers.TOGGLE_WIDGET_UI
import org.fossify.voicerecorder.models.Recording
import java.io.File
import kotlin.math.roundToLong

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.trashFolder
    get() = "${config.saveRecordingsFolder}/.trash"

fun Context.drawableToBitmap(drawable: Drawable): Bitmap {
    val size = (60 * resources.displayMetrics.density).toInt()
    val mutableBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(mutableBitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return mutableBitmap
}

fun Context.updateWidgets(isRecording: Boolean) {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext)
        ?.getAppWidgetIds(
            ComponentName(
                applicationContext,
                MyWidgetRecordDisplayProvider::class.java
            )
        ) ?: return

    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetRecordDisplayProvider::class.java).apply {
            action = TOGGLE_WIDGET_UI
            putExtra(IS_RECORDING, isRecording)
            sendBroadcast(this)
        }
    }
}

fun Context.getOrCreateTrashFolder(): String {
    val folder = File(trashFolder)
    if (!folder.exists()) {
        folder.mkdir()
    }
    return trashFolder
}

fun Context.getDefaultRecordingsFolder(): String {
    return "$internalStoragePath/$DEFAULT_RECORDINGS_FOLDER"
}

fun Context.getAllRecordings(trashed: Boolean = false): ArrayList<Recording> {
    return if (isRPlus()) {
        val recordings = arrayListOf<Recording>()
        recordings.addAll(getRecordings(trashed))
        if (trashed) {
            // Return recordings trashed using MediaStore, this won't be needed in the future
            recordings.addAll(getMediaStoreTrashedRecordings())
        }

        recordings
    } else {
        getLegacyRecordings(trashed)
    }
}

private fun Context.getRecordings(trashed: Boolean = false): ArrayList<Recording> {
    val recordings = ArrayList<Recording>()
    val folder = if (trashed) trashFolder else config.saveRecordingsFolder
    val files = getDocumentSdk30(folder)?.listFiles() ?: return recordings
    files.forEach { file ->
        if (file.isAudioRecording()) {
            recordings.add(
                readRecordingFromFile(file)
            )
        }
    }

    return recordings
}

@Deprecated(
    message = "Use getRecordings instead. This method is only here for backward compatibility.",
    replaceWith = ReplaceWith("getRecordings(trashed = true)")
)
private fun Context.getMediaStoreTrashedRecordings(): ArrayList<Recording> {
    val recordings = ArrayList<Recording>()
    val folder = config.saveRecordingsFolder
    val documentFiles = getDocumentSdk30(folder)?.listFiles() ?: return recordings
    documentFiles.forEach { file ->
        if (file.isTrashedMediaStoreRecording()) {
            val recording = readRecordingFromFile(file)
            recordings.add(
                recording.copy(
                    title = "^\\.trashed-\\d+-".toRegex().replace(file.name!!, "")
                )
            )
        }
    }

    return recordings
}

private fun Context.getLegacyRecordings(trashed: Boolean = false): ArrayList<Recording> {
    val recordings = ArrayList<Recording>()
    val folder = if (trashed) {
        trashFolder
    } else {
        config.saveRecordingsFolder
    }
    val files = File(folder).listFiles() ?: return recordings

    files.filter { it.isAudioFast() }.forEach {
        val id = it.hashCode()
        val title = it.name
        val path = it.absolutePath
        val timestamp = (it.lastModified() / 1000).toInt()
        val duration = getDuration(it.absolutePath) ?: 0
        val size = it.length().toInt()
        recordings.add(
            Recording(
                id = id,
                title = title,
                path = path,
                timestamp = timestamp,
                duration = duration,
                size = size
            )
        )
    }
    return recordings
}

private fun Context.readRecordingFromFile(file: DocumentFile): Recording {
    val id = file.hashCode()
    val title = file.name!!
    val path = file.uri.toString()
    val timestamp = (file.lastModified() / 1000).toInt()
    val duration = getDurationFromUri(file.uri)
    val size = file.length().toInt()
    return Recording(
        id = id,
        title = title,
        path = path,
        timestamp = timestamp,
        duration = duration.toInt(),
        size = size
    )
}

private fun Context.getDurationFromUri(uri: Uri): Long {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
        (time.toLong() / 1000.toDouble()).roundToLong()
    } catch (e: Exception) {
        0L
    }
}
