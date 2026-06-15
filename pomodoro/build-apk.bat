@echo off
REM One-click debug APK build for the 番茄钟 Pomodoro app.
REM Adjust these two paths if you move the JDK / Android SDK.
set "JAVA_HOME=D:\Android\jdk17"
set "ANDROID_HOME=D:\Android\sdk"
set "ANDROID_SDK_ROOT=D:\Android\sdk"

cd /d "%~dp0"
call gradlew.bat :app:assembleDebug %*
if errorlevel 1 (
    echo.
    echo BUILD FAILED.
    exit /b 1
)

copy /Y "app\build\outputs\apk\debug\app-debug.apk" "..\番茄钟-debug.apk" >nul
echo.
echo BUILD OK  ^>  ..\番茄钟-debug.apk
