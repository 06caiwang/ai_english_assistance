@echo off
chcp 65001 >nul
echo ============================================
echo  启动 english-server（本地开发）
echo ============================================
set /p MYSQL_PASSWORD=请输入 MySQL root 密码: 
if "%MYSQL_PASSWORD%"=="" (
  echo 密码不能为空
  pause
  exit /b 1
)
set SERVER__PORT=
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
pause
