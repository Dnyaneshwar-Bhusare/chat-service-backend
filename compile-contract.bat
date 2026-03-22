@echo off
echo Compiling ChatVerification.sol with --evm-version paris...
echo NOTE: Ganache is started with --hardfork paris (see start-ganache.bat)
echo       The EVM version here MUST match that hardfork.
echo.

set SOL_FILE=src\main\java\com\codingworld\service1\blockchain\ChatVerification.sol
set OUT_DIR=src\main\java\com\codingworld\service1\blockchain

solc --evm-version paris --bin --abi --optimize "%SOL_FILE%" -o "%OUT_DIR%\solc-out" --overwrite

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: solc not found or compilation failed.
    echo Install solc: https://github.com/ethereum/solidity/releases
    echo Or use: npm install -g solc
    pause
    exit /b 1
)

echo.
echo Compiled successfully. Binary:
type "%OUT_DIR%\solc-out\ChatVerification.bin"
echo.
echo Now run web3j to regenerate the Java wrapper:
echo web3j generate solidity -b "%OUT_DIR%\solc-out\ChatVerification.bin" -a "%OUT_DIR%\solc-out\ChatVerification.abi" -o src\main\java -p com.codingworld.service1.blockchain
pause
