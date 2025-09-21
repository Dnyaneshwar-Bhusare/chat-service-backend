@echo off
echo ========================================
echo   STARTING CHAT SERVICE BACKEND
echo ========================================
echo.
echo IMPORTANT: This will start the MAIN APPLICATION SERVER
echo The server will run continuously on port 9001
echo.
echo To stop the server, press Ctrl+C
echo.
echo Starting application...
echo.

cd /d "D:\desktop\college_project\cypher_squad(dev1)_G_Sachin\chat-service-backend"

echo Compiling project...
call mvnw.cmd clean compile -q

echo.
echo Starting Spring Boot application...
echo Once started, you can test at:
echo   - http://localhost:9001/
echo   - http://localhost:9001/status
echo   - http://localhost:9001/db/test
echo.

call mvnw.cmd spring-boot:run

echo.
echo Application has stopped.
pause
