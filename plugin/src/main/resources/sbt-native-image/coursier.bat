@echo off

setlocal EnableDelayedExpansion
(set LF=^
%=%
)

@REM set %HOME% to equivalent of $HOME
if "%HOME%" == "" (set HOME=%HOMEDRIVE%%HOMEPATH%)

set ERROR_CODE=0

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%" == "" goto OkJHome

for /f %%j in ("java.exe") do (
  set JAVA_EXE="%%~$PATH:j"
  goto init
)

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" (
 SET JAVA_EXE="%JAVA_HOME%\bin\java.exe"
 goto init
)

echo.
echo ERROR: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = %JAVA_HOME%
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM -- 4NT shell
if "%@eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto endInit

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto endInit

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto endInit
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

@REM Reaching here means variables are defined and arguments have been captured
:endInit

set PROG_DIR=%~dp0
@REM Remove trailing backslash from PROG_DIR
if %PROG_DIR:~-1%==\ set PROG_DIR=%PROG_DIR:~0,-1%
set JAR_PATH=%PROG_DIR%\%~n0
set PSEP=;

@REM Start Java program
:runm2
call set _JAVA_OPTS=%%JAVA_OPTS:!LF!= %%
set CMDLINE=%JAVA_EXE% %_JAVA_OPTS% -Dprog.dir="%PROG_DIR%" -jar "%JAR_PATH%" %CMD_LINE_ARGS%
%CMDLINE%
if ERRORLEVEL 1 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=1

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set JAVA_EXE=
set CMD_LINE_ARGS=
set CMDLINE=
set PSEP=
goto postExec

:endNT
@endlocal

:postExec
exit /B %ERROR_CODE%
