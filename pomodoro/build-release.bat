@echo off
REM One-click signed release APK build for the 番茄钟 Pomodoro app.
REM Requires keystore.properties + the keystore it points to.
set "JAVA_HOME=D:\Android\jdk17"
set "ANDROID_HOME=D:\Android\sdk"
set "ANDROID_SDK_ROOT=D:\Android\sdk"

cd /d "%~dp0"
call gradlew.bat :app:assembleRelease %*
if errorlevel 1 (
    echo.
    echo BUILD FAILED.
    exit /b 1
)

copy /Y "app\build\outputs\apk\release\app-release.apk" "..\番茄钟-release.apk" >nul
echo.
echo BUILD OK  ^>  ..\番茄钟-release.apk
