# 番茄钟 (Pomodoro Timer)

[![Android CI](https://github.com/user202282020008/pomodoro-android/actions/workflows/android.yml/badge.svg)](https://github.com/user202282020008/pomodoro-android/actions/workflows/android.yml)

一个标准番茄工作法计时器，原生 Android (Kotlin)。支持自定义时长、历史记录与周趋势折线图、前台后台持续计时。

## 功能
- **专注 / 短休息 / 长休息** 三阶段，每完成 N 个专注自动进入长休息
- **开始 / 暂停 / 重置 / 跳过**
- **设置页**：自定义 专注/短休息/长休息 时长与「几个专注后长休息」、**深色模式**（跟随系统/浅色/深色）、**白噪音类型**（白/棕）
- **记录页**：最近 7 天完成数 **折线图（周趋势，带绘制动画）** + 累计总数 + 每日明细，可 **导出 CSV**
- 🔊 **白噪音 / 棕噪音**：专注时播放（`AudioTrack` 实时生成，无需音频文件）；主界面一键开关
- 🌙 **深色模式**；✨ 过渡动画（阶段切换淡入、计时呼吸、图表生长）
- **持久化统计**：每天完成的番茄数用 SharedPreferences 保存，关闭/杀进程都不丢（仅自然完成的专注计入，跳过不算）
- **前台服务 + WakeLock**：息屏 / 切后台时计时继续；通知栏显示剩余时间并带 暂停·跳过·重置 按钮
- 阶段结束 **响铃 + 振动 + 通知**；不同阶段渐变配色、环形进度
- Android 13+ 运行时申请通知权限

## 技术栈
- Kotlin · 经典 XML 视图 (ViewBinding) · Material 3 · 自绘 `WeeklyChartView`（无第三方图表库）
- AGP 8.5.2 · Gradle 8.7 · Kotlin 1.9.24
- `compileSdk 34` / `minSdk 26` / `targetSdk 33`

## 源码结构
```
app/src/main/java/com/example/pomodoro/
  PomodoroEngine.kt    Phase 枚举 + TimerUiState + 阶段切换/格式化
  PomodoroSettings.kt  可配置时长/循环数（SharedPreferences）
  PomodoroStats.kt     按天持久化完成数
  TimerService.kt      前台计时服务（通知/声音/振动/自动循环）
  MainActivity.kt      主界面（观察 StateFlow，发命令）
  SettingsActivity.kt  设置页
  HistoryActivity.kt   记录页（折线图 + 列表）
  WeeklyChartView.kt   自绘 7 天折线图
app/src/main/res/      布局、字符串、颜色、主题、渐变/图标 drawable
```

## 安装
APK 已导出到上级目录：
- `..\番茄钟-debug.apk`（debug 签名，开发用）
- `..\番茄钟-release.apk`（**正式签名**，分发用）

安装方式：
- 传到手机点击安装（允许「安装未知应用」），或
- USB 调试开启后：`D:\Android\sdk\platform-tools\adb.exe install -r 番茄钟-release.apk`

> debug 与 release 用不同签名，互相覆盖安装前需先卸载旧版。

## 构建
- Debug：双击 **`build-apk.bat`**，或 `gradlew :app:assembleDebug`
- Release：双击 **`build-release.bat`**，或 `gradlew :app:assembleRelease`

构建前确保（脚本里已设好）：
```
set JAVA_HOME=D:\Android\jdk17
set ANDROID_HOME=D:\Android\sdk
```

## 正式签名（Release）
签名信息放在 **`keystore.properties`**（已被 `.gitignore` 忽略，不会提交）：
```
storeFile=D:\\Android\\keystores\\pomodoro.jks
storePassword=********
keyAlias=pomodoro
keyPassword=********
```
> 真实密码不入库 —— 见本地的 `keystore.properties`（已被 `.gitignore` 忽略）。

密钥库已生成在 `D:\Android\keystores\pomodoro.jks`（有效期 10000 天）。
如需重新生成：
```
keytool -genkeypair -v -keystore D:\Android\keystores\pomodoro.jks ^
  -storepass <pwd> -keypass <pwd> -alias pomodoro ^
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Pomodoro, O=Pomodoro, C=CN"
```
> ⚠️ 妥善保管 `pomodoro.jks` 与密码：上架/后续升级必须用**同一个**密钥签名，丢了就无法覆盖更新。

## 工具链 / 注意
- JDK 17、Android SDK、Gradle 8.7 安装在 `D:\Android`（本机原本只有 Java 8 JRE）。
- 国内网络：JDK 用微软 CDN、Gradle 用华为镜像、Maven 依赖用阿里云镜像（见 `settings.gradle.kts`）。
- 工程路径含中文「番茄」，`gradle.properties` 里设了 `android.overridePathCheck=true` 才能构建。
