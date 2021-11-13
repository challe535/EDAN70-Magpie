@REM Requires npm installed to run

call @echo off

call cd vscode
call npm list || call npm install
call vsce package || (call npm install -g vsce && call vsce package)
call code --uninstall-extension intrep-0.0.1.vsix 
Taskkill /IM code.exe /F
call code --install-extension intrep-0.0.1.vsix
call code -r
PAUSE