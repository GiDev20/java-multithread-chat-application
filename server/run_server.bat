@echo off
echo Compilazione del Server in corso...
javac -cp "..\lib\sqlite-jdbc.jar;..\lib\slf4j-api.jar;..\lib\slf4j-nop.jar;." *.java

if %ERRORLEVEL% EQU 0 (
    echo Compilazione riuscita! Avvio del Server...
    echo --------------------------------------------------
    java -cp "..\lib\sqlite-jdbc.jar;..\lib\slf4j-api.jar;..\lib\slf4j-nop.jar;." MainServer
) else (
    echo [ERRORE] Errore di compilazione. Il server non e' stato avviato.
    pause
)