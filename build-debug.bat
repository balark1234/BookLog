@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PROJECT_DIR=%~dp0"
cd /d "%PROJECT_DIR%"

echo BUILD START %DATE% %TIME%> build-status.txt

echo === Stopping Gradle daemons ===
call gradlew.bat --stop 2>nul
timeout /t 3 /nobreak >nul

if exist "app\build\kspCaches" (
    echo === Removing app\build\kspCaches ===
    rmdir /s /q "app\build\kspCaches" 2>nul
)
if exist "app\build\generated\ksp" (
    echo === Removing app\build\generated\ksp ===
    rmdir /s /q "app\build\generated\ksp" 2>nul
)

echo === Gradle assembleDebug ===
call gradlew.bat assembleDebug --no-daemon > build-log.txt 2> build-log-err.txt
if errorlevel 1 (
    echo BUILD FAILED %DATE% %TIME%>> build-status.txt
    exit /b 1
)

echo === Copying APK ===
set "APK_SRC=%LOCALAPPDATA%\BookLog-build\app\outputs\apk\debug\app-debug.apk"
if not exist "%APK_SRC%" set "APK_SRC=app\build\outputs\apk\debug\app-debug.apk"
copy /Y "%APK_SRC%" "BookLog.apk"
if errorlevel 1 (
    echo APK COPY FAILED %DATE% %TIME%>> build-status.txt
    exit /b 1
)

echo BUILD SUCCESS %DATE% %TIME%>> build-status.txt
exit /b 0