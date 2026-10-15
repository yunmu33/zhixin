package com.example.zhixin.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhixin.data.Account
import com.example.zhixin.ui.components.ChipShape
import com.example.zhixin.ui.components.PrimaryButton
import com.example.zhixin.ui.theme.LocalAppColors

@Composable
fun AccountDialog(
    context: android.content.Context,
    startWithRegister: Boolean = false,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    var register by remember { mutableStateOf(startWithRegister) }
    var name by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (register && pass != pass2) {
            error = "两次输入的密码不一样"
            return
        }
        val msg = if (register) Account.register(context, name, pass)
        else Account.login(context, name, pass)
        if (msg == null) onDismiss() else error = msg
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                if (register) "✨ 注册一个账号" else "\uD83D\uDD11 登录知新小屋",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.cardAlt)
                        .padding(4.dp)
                ) {
                    ModeTab("登录", !register, Modifier.weight(1f)) { register = false; error = null }
                    ModeTab("注册", register, Modifier.weight(1f)) { register = true; error = null }
                }
                Spacer(Modifier.height(14.dp))
                Field(name, { name = it }, "用户名", false)
                Spacer(Modifier.height(10.dp))
                Field(pass, { pass = it }, "密码", true)
                if (register) {
                    Spacer(Modifier.height(10.dp))
                    Field(pass2, { pass2 = it }, "再输一次密码", true)
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠️ $error",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.accent
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "账号只保存在这台设备上（密码做哈希处理），用来记住你的昵称、头像和打卡档案。",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkSoft
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (register) "注册并进入" else "登录",
                onClick = { submit() },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("以后再说", color = c.inkSoft) }
        }
    )
}

@Composable
private fun ModeTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.accentSoft else Color.Transparent)
            .border(
                1.dp,
                if (selected) c.accent else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) c.accent else c.inkSoft,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    password: Boolean
) {
    val c = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = c.inkSoft) },
        shape = ChipShape,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.line,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}
