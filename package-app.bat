@echo off
cd /d %~dp0
javac -cp lib\sqlite-jdbc.jar -d out src\*.java src\model\*.java src\dao\*.java src\db\*.java src\WebServer.java
if errorlevel 1 goto error
jar --create --file MercadoApp.jar -C out .
if errorlevel 1 goto error
if exist "C:\Program Files\Java\jdk-17*\bin\jpackage.exe" (
    set JPACKAGE="C:\Program Files\Java\jdk-17*\bin\jpackage.exe"
) else (
    set JPACKAGE=jpackage
)
%JPACKAGE% --input . --name MercadoSistema --main-jar MercadoApp.jar --main-class Main --type exe --app-version 1.0
if errorlevel 1 goto error
echo Aplicativo empacotado com sucesso.
exit /b 0
:error
echo Falha ao empacotar.
pause
