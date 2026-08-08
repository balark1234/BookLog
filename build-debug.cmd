@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
cd /d "%~dp0"
call gradlew.bat clean assembleDebug --no-daemon > build-output.txt 2>&1
echo EXIT_CODE=%ERRORLEVEL%>> build-output.txt
exit /b %ERRORLEVEL%