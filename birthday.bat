@echo off
setlocal
set "JAVA_HEAP_SIZE=1024"
set "SCRIPT_DIR=%~dp0"

start "" javaw -Xmx%JAVA_HEAP_SIZE%m "-Dbirthdaybot.config=%SCRIPT_DIR%..\config.properties" -jar "%SCRIPT_DIR%birthday.jar"
