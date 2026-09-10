package org.skepsun.kototoro.core.ui.dialog

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import org.acra.config.DialogConfiguration
import org.acra.config.getPluginConfiguration
import org.acra.dialog.CrashReportDialogHelper
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.util.ext.copyToClipboard

class CrashReportCopyDialog : Activity(), DialogInterface.OnClickListener {

    private lateinit var helper: CrashReportDialogHelper
    private lateinit var dialog: AlertDialog
    private var isCopying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runCatching {
            helper = CrashReportDialogHelper(this, intent)
            val configuration = helper.config.getPluginConfiguration<DialogConfiguration>()

            configuration.resTheme?.let(::setTheme)
            dialog = AlertDialog.Builder(this).apply {
                configuration.title?.takeIf { it.isNotEmpty() }?.let(::setTitle)
                configuration.resIcon?.let(::setIcon)
                configuration.text?.takeIf { it.isNotEmpty() }?.let(::setMessage)
            }
                .setPositiveButton(configuration.positiveButtonText ?: getString(R.string.copy), this)
                .setNegativeButton(configuration.negativeButtonText ?: getString(android.R.string.cancel), this)
                .create()

            dialog.setCanceledOnTouchOutside(false)
            dialog.setOnCancelListener { helper.cancelReports() }
            dialog.setOnDismissListener { finish() }
            dialog.setOnShowListener {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    copyReport()
                }
            }
            dialog.show()
        }.onFailure {
            finish()
        }
    }

    private fun copyReport() {
        if (isCopying) return

        isCopying = true
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
        Thread {
            val report = runCatching { helper.reportData.toJSON() }
            runOnUiThread {
                if (report.isSuccess) {
                    copyToClipboard(getString(R.string.crash_logs), report.getOrThrow())
                    helper.cancelReports()
                    Toast.makeText(this, R.string.crash_report_copied, Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } else {
                    isCopying = false
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
                    Toast.makeText(this, R.string.crash_report_copy_failed, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (which != DialogInterface.BUTTON_POSITIVE) {
            helper.cancelReports()
        } else {
            copyReport()
        }
    }
}
