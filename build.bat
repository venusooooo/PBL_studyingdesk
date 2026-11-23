@echo off
rem ============================================================
rem  StudyDesk  build script
rem  1) compile all sources  ->  out\
rem  2) package them        ->  StudyDesk.jar
rem  Requires JDK 17+. Double-click to build.
rem ============================================================
setlocal
cd /d "%~dp0"
title StudyDesk build

set "JAVAC=javac"
set "JAR=jar"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVAC=%JAVA_HOME%\bin\javac.exe"
    set "JAR=%JAVA_HOME%\bin\jar.exe"
)

"%JAVAC%" -version >nul 2>nul
if errorlevel 1 goto :nojava

echo [1/2] compiling sources ...
if exist out rmdir /s /q out
mkdir out
rem cmd.exe does not expand *.java, so build the source list first
( for %%f in (src\studyDesk\*.java) do @echo %%f ) > sources.txt
"%JAVAC%" -encoding UTF-8 -d out @sources.txt
if errorlevel 1 goto :failed
del /q sources.txt >nul 2>nul

echo [2/2] packaging StudyDesk.jar ...
if exist StudyDesk.jar del /q StudyDesk.jar
"%JAR%" --create --file StudyDesk.jar --main-class studyDesk.StudyDeskApp -C out .
if errorlevel 1 goto :failed
for %%f in (StudyDesk.jar) do if %%~zf LSS 10000 goto :failed

echo.
echo  Build OK: %~dp0StudyDesk.jar
echo  Double-click start.bat to run it.
echo.
exit /b 0

:nojava
echo.
echo  [!] javac not found. Install JDK 17+ and set JAVA_HOME.
echo      Download: https://adoptium.net/
echo.
pause
exit /b 1

:failed
echo.
echo  [!] Build failed, see the messages above.
echo.
if exist sources.txt del /q sources.txt >nul 2>nul
pause
exit /b 1
