/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.text.TextMMD
import com.paperapps.paperui.components.DashedDivider
import moe.rukamori.archivetune.constants.SongSortType

/**
 * CUSTOM: E-Ink Song Sort Sheet
 *
 * A bottom sheet that lets users pick the sort order for the Songs library tab.
 * Follows the same visual pattern as the rest of the eink bottom sheet system.
 */
@Composable
fun EinkSongSortSheet(
    currentSort: SongSortType,
    isDescending: Boolean,
    onSortSelected: (SongSortType, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        SongSortType.NAME to "Name",
        SongSortType.CREATE_DATE to "Date Added",
        SongSortType.PLAY_TIME to "Most Played",
    )

    ModalBottomSheetMMD(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            TextMMD(
                text = "Sort Songs",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            options.forEach { (type, label) ->
                val isSelected = currentSort == type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Tapping the active sort type toggles ascending/descending
                            val newDescending = if (isSelected) !isDescending else false
                            onSortSelected(type, newDescending)
                            onDismiss()
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextMMD(
                        text = label,
                        fontSize = 20.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        // Show ascending/descending indicator
                        TextMMD(
                            text = if (isDescending) "↓" else "↑",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                        )
                    }
                }
                DashedDivider(thickness = 1.dp)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
