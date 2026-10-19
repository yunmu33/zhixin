package com.example.zhixin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zhixin.ui.theme.LocalAppColors

/**
 * 挑任务颜色的面板：按色系一排排摆好，最后一个星号项是「自动」（跟着列表顺序轮换）。
 *
 * @param picked 当前选中的下标，-1 = 自动
 * @param onPick 传出下标，自动那一项传 -1
 */
@Composable
fun TaskColorPicker(
    picked: Int,
    onPick: (Int) -> Unit
) {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TASK_COLOR_GROUPS.forEach { (label, indices) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(56.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (label == "经典") {
                        ColorDot(
                            color = null,
                            picked = picked < 0,
                            onClick = { onPick(-1) },
                            size = 32.dp
                        )
                        Text(
                            "自动",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                    } else {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkSoft
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    indices.forEach { i ->
                        ColorDot(
                            color = androidx.compose.ui.graphics.Color(TASK_COLORS[i]),
                            picked = picked == i,
                            onClick = { onPick(i) },
                            size = 32.dp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "点「自动」就跟着任务顺序轮换颜色",
            style = MaterialTheme.typography.labelSmall,
            color = c.inkSoft
        )
    }
}
