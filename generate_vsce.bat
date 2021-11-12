@REM Requires npm installed to run
call cd vscode
call npm list || npm install
call vsce package || (npm install -g vsce && vsce package)
call code --install-extension intrep-0.0.1.vsix || (code --uninstall-extension intrep-0.0.1.vsix && Taskkill /IM code.exe /F && code -r && code --install-extension intrep-0.0.1.vsix)
PAUSE