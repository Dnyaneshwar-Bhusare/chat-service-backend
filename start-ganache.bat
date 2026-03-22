@echo off
echo Starting Ganache with Paris hardfork...
echo NOTE: Contract binary is compiled with --evm-version paris (see compile-contract.bat)
echo       The hardfork here MUST match that evm-version.
echo.

REM Try Ganache CLI first (npm package)
where npx >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Using npx ganache...
    npx ganache --port 7545 --hardfork paris --accounts 10 --deterministic --gas-limit 6721975 --gas-price 20000000000
    goto :end
)

REM Try global ganache install
where ganache >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Using global ganache...
    ganache --port 7545 --hardfork paris --accounts 10 --deterministic --gas-limit 6721975 --gas-price 20000000000
    goto :end
)

REM Try ganache-cli (older version)
where ganache-cli >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Using ganache-cli...
    ganache-cli --port 7545 --hardfork paris --accounts 10 --deterministic --gasLimit 6721975 --gasPrice 20000000000
    goto :end
)

echo.
echo ERROR: ganache not found in PATH.
echo Install it with:  npm install -g ganache
echo Then restart this script.
pause
exit /b 1

:end
