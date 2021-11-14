# Build instructions

## Building for VS Code

***Make sure you save any work currently open in VS Code before continuing, as the following steps will force a restart of VS Code.***

You need to have Node.js and be able to execute `npm` commands in the console.

1. Initalize all required git submodules with the command `git submodule update --init --recursive`
2. Execute the `vsce` task with gradlew by
   - if you're on windows type `gradlew vsce` into the command line.
   - if you're on linux type `./gradlew vsce` into the command line.

That should automatically build, package, and install the extension to VS Code. If it didn't work, you can look for and manually install the .vsix file generated in the 'vscode' directory. If you can't find any .vsix file try executing above steps one more time.

**Currently you will need to run `gradlew vsce` twice first time you install the extension for it to work properly** 