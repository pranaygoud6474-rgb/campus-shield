@echo off
setlocal
set "JDK_ROOT=%USERPROFILE%\.jdk\campus-safety-jdk17"
for /d %%J in ("%JDK_ROOT%\jdk-17*") do set "JAVA_HOME=%%~fJ"
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Java 17 was not found under "%JDK_ROOT%".
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
if not defined JWT_SECRET (
  echo E2E runner did not provide a temporary JWT signing secret.
  exit /b 1
)
if not defined BOOTSTRAP_ADMIN_PASSWORD (
  echo E2E runner did not provide a temporary bootstrap password.
  exit /b 1
)
set APP_PROFILE=test
set SERVER_PORT=8080
set BOOTSTRAP_ADMIN_ENABLED=true
set BOOTSTRAP_ADMIN_EMAIL=e2e-admin@example.edu
cd /d "%~dp0..\..\campus-safety-backend"
echo Starting isolated H2 test backend with Java 17.
call mvn.cmd spring-boot:run -Dspring-boot.run.profiles=test
