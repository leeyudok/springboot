@echo off
chcp 65001 > nul
powershell -Command "& { $OutputEncoding = [System.Text.Encoding]::UTF8; .\gradlew.bat bootRun --args='--spring.profiles.active=local' }"