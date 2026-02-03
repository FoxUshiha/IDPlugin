@echo off
setlocal ENABLEDELAYEDEXPANSION
chcp 65001 >nul
pushd "%~dp0"

title Compilador ID-Plugin v1.0
color 0A

REM ================= CONFIG =================
set "PLUGIN_NAME=IDPlugin"
set "OUTJAR=%PLUGIN_NAME%.jar"
set "SRC_ROOT=src"
set "MAIN_CLASS=com.foxsrv.id.ID"

REM ================= BANNER =================
echo.
echo ========================================
echo        COMPILADOR ID-PLUGIN
echo ========================================
echo.

REM ================= PREP ===================
echo [1/6] Preparando diretorios...
if not exist out mkdir out
if not exist out\classes mkdir out\classes
if exist out\%OUTJAR% (
    echo   Removendo JAR antigo...
    del /q out\%OUTJAR%
)

REM ================= AMBIENTE =================
echo [2/6] Verificando ambiente Java...
where javac >nul 2>nul
if errorlevel 1 (
    echo [ERRO] javac nao encontrado. Instale JDK 17 ou configure JAVA_HOME.
    pause
    popd
    exit /b 1
)

REM ================= VERIFICAR DEPENDÊNCIAS =================
echo [3/6] Verificando dependencias...

set "CP=."
set "DEPS_OK=1"

if not exist "spigot-api-1.20.1-R0.1-SNAPSHOT.jar" (
    echo [ERRO] Spigot API nao encontrada: spigot-api-1.20.1-R0.1-SNAPSHOT.jar
    set "DEPS_OK=0"
) else (
    set "CP=!CP!;spigot-api-1.20.1-R0.1-SNAPSHOT.jar"
    echo   [+] Spigot API encontrada
)

if exist "Vault.jar" (
    set "CP=!CP!;Vault.jar"
    echo   [+] Vault encontrado
) else (
    echo   [AVISO] Vault.jar nao encontrado (opcional)
)

if exist "PlaceholderAPI.jar" (
    set "CP=!CP!;PlaceholderAPI.jar"
    echo   [+] PlaceholderAPI encontrado (opcional)
) else (
    echo   [AVISO] PlaceholderAPI.jar nao encontrado (opcional)
)

if "!DEPS_OK!"=="0" (
    pause
    popd
    exit /b 1
)

REM ================= COLETAR FONTES =================
echo [4/6] Coletando arquivos .java...
set "FILES="
set "COUNT=0"

for /R "%SRC_ROOT%" %%F in (*.java) do (
    set /a COUNT+=1
    set "FILES=!FILES! "%%~fF""
)

if "!FILES!"=="" (
    echo [ERRO] Nenhum arquivo .java encontrado em %SRC_ROOT%
    pause
    popd
    exit /b 1
)

echo   Encontrados !COUNT! arquivo(s) .java

REM ================= COMPILAR =================
echo [5/6] Compilando %PLUGIN_NAME%...

javac ^
  -encoding UTF-8 ^
  --release 17 ^
  -Xlint:deprecation ^
  -classpath "!CP!" ^
  -d out\classes ^
  !FILES!

if errorlevel 1 (
    echo.
    echo [ERRO] Falha na compilacao.
    echo Verifique os erros acima.
    pause
    popd
    exit /b 1
)

echo   [+] Compilacao bem-sucedida

REM ================= COPIAR RESOURCES =================
echo [6/6] Copiando resources e criando JAR...
if exist resources (
    echo   Copiando resources...
    xcopy /E /Y /I resources out\classes >nul
    if exist out\classes\plugin.yml (
        echo   [+] plugin.yml copiado
    )
    if exist out\classes\config.yml (
        echo   [+] config.yml copiado
    )
    if exist out\classes\users.yml (
        echo   [+] users.yml copiado
    )
) else (
    echo [ERRO] Pasta resources nao encontrada.
    pause
    popd
    exit /b 1
)

REM ================= VERIFICAR PLUGIN.YML =================
echo   Verificando plugin.yml...
if exist out\classes\plugin.yml (
    findstr /C:"main: %MAIN_CLASS%" out\classes\plugin.yml >nul
    if errorlevel 1 (
        echo [AVISO] main class pode nao corresponder em plugin.yml
        echo   Esperado: main: %MAIN_CLASS%
        type out\classes\plugin.yml | findstr "main:"
    ) else (
        echo   [+] Main class correta
    )
)

REM ================= EMPACOTAR =================
echo   Criando JAR: %OUTJAR%...

set "JARCMD="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JARCMD=%JAVA_HOME%\bin\jar.exe"
if not defined JARCMD where jar >nul 2>nul && set "JARCMD=jar"

if not defined JARCMD (
    echo [ERRO] comando jar nao encontrado.
    pause
    popd
    exit /b 1
)

pushd out\classes
"%JARCMD%" cf ..\%OUTJAR% * 2>nul
popd

REM ================= VERIFICAR JAR =================
if exist out\%OUTJAR% (
    for %%F in (out\%OUTJAR%) do set "JAR_SIZE=%%~zF"
    set /a JAR_SIZE_KB=JAR_SIZE/1024
    echo   [+] JAR criado: %OUTJAR% (!JAR_SIZE_KB! KB)
    
    REM Verificar se contem os arquivos necessarios
    "%JARCMD%" tf out\%OUTJAR% | findstr /C:"plugin.yml" >nul
    if errorlevel 0 (
        echo   [+] plugin.yml incluido
    ) else (
        echo [ERRO] plugin.yml nao encontrado no JAR
    )
    
    "%JARCMD%" tf out\%OUTJAR% | findstr /C:"com/foxsrv/id/ID.class" >nul
    if errorlevel 0 (
        echo   [+] Classe principal incluida
    ) else (
        echo [ERRO] Classe principal nao encontrada no JAR
    )
) else (
    echo [ERRO] Falha ao criar JAR
    pause
    popd
    exit /b 1
)

REM ================= FINAL =================
echo.
echo ========================================
echo BUILD CONCLUIDO COM SUCESSO!
echo ========================================
echo.
echo Arquivo gerado: out\%OUTJAR%
echo.
echo Instrucoes:
echo 1. Copie "out\%OUTJAR%" para pasta plugins
echo 2. Reinicie/reload servidor
echo 3. Plugin dependencias opcionais:
echo    - PlaceholderAPI (para placeholders extras)
echo    - Vault (se necessario para economia)
echo.
pause
popd
