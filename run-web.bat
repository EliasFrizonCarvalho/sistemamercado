@echo off
cd /d %~dp0
javac -cp lib\sqlite-jdbc.jar -d out src\*.java src\model\*.java src\dao\*.java src\db\*.java src\WebServer.java
if errorlevel 1 goto error
java -cp "out;lib\sqlite-jdbc.jar" WebServer
exit /b 0
:error
echo Compilação falhou.
pause
