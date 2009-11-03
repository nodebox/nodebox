; NodeBox installer script for use with NSIS (Nullsoft Scriptable Install System).

; Modern installer
!include "MUI2.nsh"

;--------------------------------

; General
Name "NodeBox"
OutFile "dist\nodebox-958.exe"
InstallDir "$LOCALAPPDATA\NodeBox\Application"
InstallDirRegKey HKCU "Software\NodeBox" ""
RequestExecutionLevel user

;--------------------------------
; Interface settings
!define MUI_ICON "platform\windows\res\nodebox.ico"

;--------------------------------

; Pages
!insertmacro MUI_PAGE_LICENSE "License.rtf"
!insertmacro MUI_PAGE_INSTFILES

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

;--------------------------------

; The stuff to install
Section ""
  SetOutPath $INSTDIR
  ; Copy all application resources.
  File /r dist\windows\NodeBox\*.*
  ; Store installation folder
  WriteRegStr HKCU "Software\NodeBox" "" $INSTDIR
  ; Create a start menu entry.
  CreateShortCut $SMPROGRAMS\NodeBox.lnk $INSTDIR\nodebox.exe
  ; Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  ; Write uninstaller to add/remove programs
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox" "DisplayName" "NodeBox"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox" "DisplayIcon" "$INSTDIR\NodeBox.exe"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox" "Publisher" "Experimental Media Group"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox" "InstallLocation" "$INSTDIR"
SectionEnd

Section "Uninstall"
  RMDir /r "$INSTDIR"
  DeleteRegKey /ifempty HKCU "Software\NodeBox"
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox"
SectionEnd