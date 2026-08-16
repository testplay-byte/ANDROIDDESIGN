package com.confused.onlylist.error

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.confused.onlylist.MainActivity
import com.confused.onlylist.designsystem.components.pressScale
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography
import java.io.File

/**
 * ErrorActivity — shown when the app crashes.
 * Per CORE_RULES §19: shows "Something went wrong" + scrollable crash log + Copy + Restart + Close.
 */
class ErrorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashReport = try {
            File(filesDir, "last_crash.txt").readText()
        } catch (e: Exception) {
            "No crash report available."
        }

        setContent {
            val colors = LocalColors.current
            val typography = LocalTypography.current
            val context = LocalContext.current

            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(24.dp)
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Error icon
                    BasicText(
                        text = "⚠",
                        style = typography.displayLarge.copy(color = colors.error),
                    )

                    // Title
                    BasicText(
                        text = "Something went wrong",
                        style = typography.displayMedium.copy(color = colors.textPrimary),
                    )

                    // Explanation
                    BasicText(
                        text = "Only-List encountered an unexpected error. You can copy the crash log below and report it, or restart the app.",
                        style = typography.bodyMedium.copy(color = colors.textSecondary),
                    )

                    // Crash log (scrollable)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(colors.surfaceVariant)
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        BasicText(
                            text = crashReport,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                            ),
                        )
                    }

                    // Buttons
                    val shapes = LocalShapes.current
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .pressScale {
                                    copyToClipboard(context, crashReport)
                                }
                                .background(colors.primaryMuted, shapes.medium)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "Copy Log",
                                style = typography.titleMedium.copy(color = colors.primary),
                            )
                        }

                        Box(
                            Modifier
                                .weight(1f)
                                .pressScale {
                                    clearCrashAndRestart(context)
                                }
                                .background(colors.primary, shapes.medium)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "Restart",
                                style = typography.titleMedium.copy(color = colors.onPrimary),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Only-List crash log", text))
    }

    private fun clearCrashAndRestart(context: Context) {
        try {
            File(filesDir, "last_crash.txt").delete()
        } catch (e: Exception) { /* ignore */ }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        finish()
    }
}
