@echo off
setlocal

set MAVEN_VERSION=3.9.6
set MAVEN_HOME=%~dp0.mvn\apache-maven-%MAVEN_VERSION%
set MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd

if exist "%MAVEN_BIN%" goto execute

echo Downloading Maven %MAVEN_VERSION%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$zip = '%~dp0.mvn\maven.zip';" ^
  "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip;" ^
  "Expand-Archive -Path $zip -DestinationPath '%~dp0.mvn' -Force;" ^
  "Remove-Item $zip;"

if not exist "%MAVEN_BIN%" (
  echo Failed to set up Maven. Check your internet connection.
  exit /b 1
)
echo Maven ready.

:execute
"%MAVEN_BIN%" %*
endlocal
