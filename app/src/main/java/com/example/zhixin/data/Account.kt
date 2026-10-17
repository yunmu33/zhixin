package com.example.zhixin.data

import android.content.Context
import java.security.MessageDigest

/**
 * 本机账号：注册 / 登录 / 退出。
 * 账号与密码哈希只写在这台设备的偏好里，没有服务器、不会上传。
 */
object Account {

    fun loggedIn(settings: AppSettings): Boolean = settings.accountUser.isNotBlank()

    private fun hash(pass: String): String = MessageDigest.getInstance("SHA-256")
        .digest(pass.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    /** 返回 null 表示成功，否则是给用户的提示。 */
    fun register(context: Context, name: String, pass: String): String? {
        val n = name.trim()
        if (n.length < 2) return "用户名至少要 2 个字"
        if (pass.length < 4) return "密码至少要 4 位"
        val s = Prefs.settings.value
        if (s.accounts.containsKey(n)) return "这个用户名已经被注册啦"
        Prefs.update(
            context,
            s.copy(
                accounts = s.accounts + (n to hash(pass)),
                accountUser = n,
                nickname = if (s.nickname.isBlank() || s.nickname == "旅人") n else s.nickname
            )
        )
        return null
    }

    fun login(context: Context, name: String, pass: String): String? {
        val s = Prefs.settings.value
        val n = name.trim()
        val stored = s.accounts[n] ?: return "还没有这个账号，先注册一个吧"
        if (stored != hash(pass)) return "密码不对哦，再试一次"
        Prefs.update(context, s.copy(accountUser = n))
        return null
    }

    fun logout(context: Context) {
        Prefs.patch(context) { it.copy(accountUser = "") }
    }
}
