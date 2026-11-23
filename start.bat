@echo off
rem ============================================================
rem  StudyDesk  launcher  (double-click to run)
rem  Builds StudyDesk.jar automatically on first run.
rem  Requires JDK 17+ (or JAVA_HOME pointing at one).
rem ============================================================
setlocal
cd /d "%~dp0"
title StudyDesk

set "JAVA_EXE=java"
set "JAVAW_EXE=javaw"
set "JAVAC_EXE=javac"
set "JAR_EXE=jar"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    set "JAVAW_EXE=%JAVA_HOME%\bin\javaw.exe"
    set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
    set "JAR_EXE=%JAVA_HOME%\bin\jar.exe"
)

"%JAVA_EXE%" -version >nul 2>nul
if errorlevel 1 goto :nojava

if not exist "StudyDesk.jar" (
    echo  First run: building StudyDesk.jar, please wait ...
    if exist out rmdir /s /q out
    mkdir out
    ( for %%f in (src\studyDesk\*.java) do @echo %%f ) > sources.txt
    "%JAVAC_EXE%" -encoding UTF-8 -d out @sources.txt
    if errorlevel 1 goto :buildfail
    del /q sources.txt >nul 2>nul
    "%JAR_EXE%" --create --file StudyDesk.jar --main-class studyDesk.StudyDeskApp -C out .
    if errorlevel 1 goto :buildfail
    echo  Build OK.
)

start "" "%JAVAW_EXE%" -Dfile.encoding=UTF-8 -jar "%~dp0StudyDesk.jar"
exit /b 0

:nojava
echo.
echo  [!] Java not found.
echo      Install JDK 17 or newer, then run this file again.
echo      Download: https://adoptium.net/
echo.
pause
exit /b 1

:buildfail
echo.
echo  [!] Build failed. Run build.bat to see the details.
echo.
pause
exit /b 1
