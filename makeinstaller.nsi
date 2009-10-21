; NodeBox installer script for use with NSIS (Nullsoft Scriptable Install System).

Name "NodeBox"
OutFile "dist\nodebox-957.exe"
InstallDir $PROGRAMFILES\NodeBox
RequestExecutionLevel user

;--------------------------------

; Pages
Page directory
Page instfiles

;--------------------------------

; The stuff to install
Section ""
  SetOutPath $INSTDIR
  ; Copy all application resources.
  File /r dist\windows\NodeBox\*.*
  ; Create a start menu entry.
  CreateShortCut $SMPROGRAMS\NodeBox.lnk $INSTDIR\nodebox.exe
SectionEnd
