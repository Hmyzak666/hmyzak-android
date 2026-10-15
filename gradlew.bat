@echo off
setlocal
cd /d "%~dp0"
set "WRAPPER=gradle\wrapper\gradle-wrapper.jar"
if not exist "%WRAPPER%" (
  echo Downloading official Gradle 8.13 wrapper...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://services.gradle.org/distributions/gradle-8.13-wrapper.jar'; $dest='gradle/wrapper/gradle-wrapper.jar'; Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile ($dest+'.download'); $hash=(Invoke-WebRequest -UseBasicParsing -Uri ($url+'.sha256')).Content.Trim().ToLowerInvariant(); $got=(Get-FileHash -Algorithm SHA256 ($dest+'.download')).Hash.ToLowerInvariant(); if($got -ne $hash) { Remove-Item ($dest+'.download'); throw 'Checksum verification failed' }; Move-Item -Force ($dest+'.download') $dest"
  if errorlevel 1 exit /b 1
)
java -classpath "%WRAPPER%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
