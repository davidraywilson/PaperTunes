/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.papertunes.eink.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD

val LocalEinkMenuState = compositionLocalOf { EinkMenuState() }
val LocalEinkBottomSheetPageState = compositionLocalOf { EinkBottomSheetPageState() }

@Stable
class EinkMenuState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)

    fun show(content: @Composable ColumnScope.() -> Unit) {
        isVisible = true
        this.content = content
    }

    fun dismiss() {
        isVisible = false
    }
}

@Stable
class EinkBottomSheetPageState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)

    fun show(content: @Composable ColumnScope.() -> Unit) {
        isVisible = true
        this.content = content
    }

    fun dismiss() {
        isVisible = false
    }
}

@Composable
fun EinkBottomSheetMenu(
    modifier: Modifier = Modifier,
    state: EinkMenuState,
) {
    val focusManager = LocalFocusManager.current

    if (state.isVisible) {
        ModalBottomSheetMMD(
            onDismissRequest = {
                focusManager.clearFocus()
                state.isVisible = false
            },
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                state.content(this)
            }
        }
    }
}

@Composable
fun EinkBottomSheetPage(
    modifier: Modifier = Modifier,
    state: EinkBottomSheetPageState,
) {
    val focusManager = LocalFocusManager.current

    if (state.isVisible) {
        // MMD's ModalBottomSheet is used for full pages as well in the E-ink variant
        ModalBottomSheetMMD(
            onDismissRequest = {
                focusManager.clearFocus()
                state.isVisible = false
            },
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                state.content(this)
            }
        }
    }
}
