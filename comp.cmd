@echo off
setlocal enabledelayedexpansion

REM Determine project root (directory of this script)
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

set JAR=target\folderCompare-1.0-SNAPSHOT-shaded.jar

if not exist "%JAR%" (
  echo Shaded JAR not found, building...
  call mvnw.cmd -q -DskipTests package
  if errorlevel 1 (
    echo Build failed.
    exit /b 1
  )
)

echo Running folderCompare...
java -jar "%JAR%" %*
