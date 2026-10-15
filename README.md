# 知新 · 情绪小屋

一句话科普：一款**纯本地**的心情打卡与日记 App。记录每天的心情、任务和「新收获」，用热度值看到自己的坚持轨迹。

> 所有数据只保存在这台设备上，不上传任何云端。

---

## 功能一览

五个底部页签：

| 页签 | 内容 |
|---|---|
| 🏠 主页 | 热度总览、三个打卡入口（每日一新 / 任务打卡 / 心情打卡）、打卡提醒、小屋书桌、最近的小故事 |
| 📅 日历 | 月历打卡色块、近 30 天情绪趋势、心情分布、年度热力图（日 / 周 / 月 / 年视图切换） |
| 🌱 每日一新 | 每天记一条新收获，哪怕一句话 |
| 📒 收藏本 | 全部记录，支持搜索、编辑、回收站、导出 / 导入备份 |
| 👤 我的 | 头像昵称签名、数据概览、成就徽章、设置入口、数据备份 |

### 打卡方式

- **主页三个入口卡**，点一下弹出对应内容：
  - 🌱 每日一新 → 输入一句话即可保存，**立即显示成功反馈**
  - 🎯 任务打卡 → 自定义任务网格，点一下完成 / 再点取消 / 长按删除，可随时添加任务
  - 😊 心情打卡 → 选心情 + 勾事件 + 写一句 →「完成打卡」→ **🎉 打卡成功**（显示今日热度、连续天数、累计热度）
- **完整打卡页**（心情打卡弹窗底部「打卡日历 + 心情日记 →」进入）：上半部分是打卡日历（可补打过去任意一天），下半部分是心情日记打卡。
- 热度规则：每次打卡 +10，连续打卡有最高 +20 的加成；等级依次是 未点燃 → 微光 → 温热 → 火热 → 炽热 → 爆表。

---

## 版本

当前版本：**v1.0.0**（`versionCode = 1`，`versionName = "1.0.0"`，见 `app/build.gradle.kts`）。

应用内「我的」页底部会自动显示当前版本号（读取 `BuildConfig.VERSION_NAME`，改一处即可）。

---

## 覆盖下载更新（不丢数据）

只要满足下面三个条件，新 APK 就能直接覆盖安装升级，**已有记录全部保留**：

1. **包名不变**：`com.example.zhixin`
2. **签名一致**：所有版本都用项目根目录的 `zhixin.jks` 签名（debug 与 release 已配置同一个签名，见 `app/build.gradle.kts` 的 `signingConfigs`）
3. **versionCode 递增**：新版本的 `versionCode` 必须比手机上已安装的大

### 发布新版本的步骤

1. 修改 `app/build.gradle.kts`：

```kotlin
versionCode = 2        // 必须 +1（或更大）
versionName = "1.0.1"  // 同步更新
```

2. 打包：

```
gradlew assembleRelease
# 输出：app/build/outputs/apk/release/app-release.apk
```

3. 安装升级（任选其一）：
   - 手机下载 APK 后点击安装
   - 或连 USB 执行 `adb install -r app-release.apk`

> ⚠️ **Keystore 文件 `zhixin.jks` 必须保存好且不要更换。**一旦丢失或换用别的签名，用户就必须卸载重装（数据会丢）。
> 别名 / 密码：
> - `storeFile = zhixin.jks`，`storePassword = zhixin2026`
> - `keyAlias = zhixin`，`keyPassword = zhixin2026`

### 从旧版（1.0.0 之前的 debug 签名包）升级

旧包用的是 Android Studio 默认的 debug 签名，与 `zhixin.jks` 不一致，**第一次需要卸载重装一次**：

```
adb uninstall com.example.zhixin
adb install app-release.apk
```

之后的版本都能正常覆盖升级。建议卸载前先在「设置 → 导出」备份数据，装好后「导入」即可恢复。

---

## 项目结构

```
app/src/main/java/com/example/zhixin/
├── data/           数据层：Entry 模型、日期工具 DateKey、热度计算 Heat、偏好 Prefs、存储 Store、账号 Account
├── notify/         每日打卡提醒
├── ui/
│   ├── components/ 通用组件：卡片、图表 Charts、月历 MonthGrid、对话框
│   ├── screens/    各个页面：HomeScreen、CheckInScreen、CalendarScreen、NewScreen、CollectScreen、
│   │               MineScreen、SettingsScreen、HomeQuickSheet（主页三个弹窗）…
│   ├── theme/      主题、皮肤、配色
│   └── AppRoot.kt  全局路由与五个页签
└── MainActivity.kt
```

## 构建要求

- Android Gradle Plugin（Kotlin DSL）+ Kotlin Compose 插件
- `compileSdk 37` / `minSdk 24` / `targetSdk 37`
- JDK 11 及以上

## 常用命令

```bash
gradlew assembleDebug     # 调试包
gradlew assembleRelease   # 发布包（用 zhixin.jks 签名）
gradlew installDebug      # 直接装到连接的设备
```
