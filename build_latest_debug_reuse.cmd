@echo off
setlocal
set "GRADLE_USER_HOME=C:\mtgradle1"
set "TMP=C:\mttmp1"
set "TEMP=C:\mttmp1"
call gradlew.bat :app:assembleDebug --no-daemon -Dorg.gradle.vfs.watch=false
endlocal
