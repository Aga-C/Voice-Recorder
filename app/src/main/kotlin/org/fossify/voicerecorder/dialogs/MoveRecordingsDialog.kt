package org.fossify.voicerecorder.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.voicerecorder.R
import org.fossify.voicerecorder.databinding.DialogMoveRecordingsBinding
import org.fossify.voicerecorder.extensions.getAllRecordings
import org.fossify.voicerecorder.extensions.moveRecordings

class MoveRecordingsDialog(
    private val activity: BaseSimpleActivity,
    private val previousFolder: String,
    private val newFolder: String,
    private val callback: () -> Unit
) {
    private lateinit var dialog: AlertDialog
    private val binding = DialogMoveRecordingsBinding.inflate(activity.layoutInflater).apply {
        message.setText(R.string.move_recordings_to_new_folder_desc)
        progressIndicator.setIndicatorColor(activity.getProperPrimaryColor())
    }

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.yes, null)
            .setNegativeButton(org.fossify.commons.R.string.no, null)
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    titleId = R.string.move_recordings
                ) {
                    dialog = it
                    dialog.setOnDismissListener { callback() }
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                        callback()
                        dialog.dismiss()
                    }

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        binding.progressIndicator.show()
                        with(dialog) {
                            setCancelable(false)
                            setCanceledOnTouchOutside(false)
                            arrayOf(
                                binding.message,
                                getButton(AlertDialog.BUTTON_POSITIVE),
                                getButton(AlertDialog.BUTTON_NEGATIVE)
                            ).forEach { button ->
                                button.isEnabled = false
                                button.alpha = 0.6f
                            }

                            moveAllRecordings()
                        }
                    }
                }
            }
    }

    private fun moveAllRecordings() {
        ensureBackgroundThread {
            activity.moveRecordings(
                recordingsToMove = activity.getAllRecordings(),
                sourceParent = previousFolder,
                destinationParent = newFolder
            ) {
                activity.runOnUiThread {
                    callback()
                    dialog.dismiss()
                }
            }
        }
    }
}