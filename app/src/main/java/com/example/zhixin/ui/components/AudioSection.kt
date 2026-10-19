package com.example.zhixin.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.zhixin.data.AudioStore
import com.example.zhixin.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/** 秒数显示成 00:05 这种样子。 */
private fun clock(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

/**
 * 一段语音区：
 * - 编辑态：上面是录音按钮（点一下开始，再点停止并保存），下面是已录的语音
 * - 查看态：只列出来，点一条就播放，再点一下停
 */
@Composable
fun AudioSection(
    files: List<String>,
    editing: Boolean,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    val context = LocalContext.current

    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf<String?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var pendingFile by remember { mutableStateOf<String?>(null) }
    // 播放到第几秒，用来显示 00:01 / 00:03
    var progress by remember { mutableIntStateOf(0) }

    fun stopPlay() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        playing = null
    }

    fun startPlay(name: String) {
        stopPlay()
        val f = AudioStore.file(context, name)
        if (!f.exists()) return
        runCatching {
            val mp = MediaPlayer().apply {
                setDataSource(f.absolutePath)
                prepare()
                start()
            }
            mp.setOnCompletionListener { stopPlay() }
            player = mp
            playing = name
        }
    }

    fun startRecord() {
        stopPlay()
        val name = AudioStore.nextName()
        val f = AudioStore.file(context, name)
        runCatching {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(f.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            pendingFile = name
            recording = true
            seconds = 0
        }
    }

    fun stopRecord(save: Boolean) {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        val name = pendingFile
        pendingFile = null
        recording = false
        seconds = 0
        if (name == null) return
        // 太短或没录上就直接丢掉，不留空文件
        if (save && AudioStore.file(context, name).length() > 0L) {
            onAdd(name)
        } else {
            AudioStore.delete(context, name)
        }
    }

    // 录音时每秒走一格
    LaunchedEffect(recording) {
        while (recording) {
            delay(1000)
            seconds++
        }
    }

    // 播放时刷新进度，用来显示 00:01 / 00:03
    LaunchedEffect(playing) {
        while (playing != null) {
            progress = (player?.currentPosition ?: 0) / 1000
            delay(200)
        }
        progress = 0
    }

    // 离开这一页就停掉录音和播放，别在后台还开着麦
    DisposableEffect(Unit) {
        onDispose {
            if (recording) {
                runCatching { recorder?.stop() }
                runCatching { recorder?.release() }
                recorder = null
                pendingFile?.let { AudioStore.delete(context, it) }
            }
            stopPlay()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecord()
    }

    fun recordOrStop() {
        if (recording) {
            stopRecord(save = true)
            return
        }
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRecord()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (editing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (recording) c.accent.copy(alpha = 0.16f) else c.cardAlt)
                    .border(
                        width = 1.dp,
                        color = if (recording) c.accent else c.line,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { recordOrStop() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (recording) "⏹ 说完了，点这保存（${clock(seconds)}）"
                    else "\uD83C\uDF99 点一下开始说话",
                    fontSize = 14.sp,
                    color = if (recording) c.accent else c.ink
                )
            }
        }

        if (files.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            files.forEachIndexed { index, name ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.card)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.cardAlt)
                            .clickable {
                                if (playing == name) stopPlay() else startPlay(name)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (playing == name) "⏸" else "▶", fontSize = 14.sp, color = c.ink)
                    }
                    Spacer(Modifier.width(10.dp))
                    // 时长从文件里读出来；正在播的那条再显示进度
                    val totalSec = remember(name) {
                        (AudioStore.durationMillis(context, name) / 1000).toInt()
                    }
                    val timeText = if (playing == name && totalSec > 0) {
                        "${clock(progress.coerceAtMost(totalSec))} / ${clock(totalSec)}"
                    } else if (totalSec > 0) {
                        clock(totalSec)
                    } else {
                        ""
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "语音 ${index + 1}",
                            fontSize = 14.sp,
                            color = c.ink
                        )
                        if (timeText.isNotEmpty()) {
                            Text(
                                timeText,
                                fontSize = 11.sp,
                                color = if (playing == name) c.accent else c.inkSoft
                            )
                        }
                    }
                    if (editing) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(c.cardAlt)
                                .clickable {
                                    if (playing == name) stopPlay()
                                    AudioStore.delete(context, name)
                                    onDelete(name)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontSize = 13.sp, color = c.inkSoft)
                        }
                    }
                }
            }
        }

        if (editing && files.isEmpty() && !recording) {
            Spacer(Modifier.height(6.dp))
            Text(
                "说不出来也可以说出来，之后点就能听",
                fontSize = 12.sp,
                color = c.inkSoft,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
