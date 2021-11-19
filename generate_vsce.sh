cd vscode
npm list | npm install
vsce package | npm install -g vsce && vsce package
cp ../build/libs/IntRep.jar  IntRep.jar 
# code --uninstall-extension intrep-0.0.1.vsix 
code --install-extension intrep-0.0.1.vsix
code -r