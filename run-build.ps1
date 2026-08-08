$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
& .\gradlew.bat assembleDebug --no-daemon 2>&1 | Tee-Object -FilePath (Join-Path $PSScriptRoot 'build-output.txt')
exit $LASTEXITCODE