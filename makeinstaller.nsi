; NodeBox installer script for use with NSIS (Nullsoft Scriptable Install System).

; Modern installer
!include "MUI2.nsh"

;--------------------------------

; Application metadata
!define APPNAME "NodeBox"
!define APPVERSION "958"
!define APPNAMEANDVERSION "NodeBox v${APPVERSION}"
!define APPWEBSITE "http://beta.nodebox.net/"

; General
Name "${APPNAMEANDVERSION}"
OutFile "dist\nodebox-${APPVERSION}.exe"
InstallDir "$LOCALAPPDATA\${APPNAME}\Application"
InstallDirRegKey HKCU "Software\${APPNAME}" ""
RequestExecutionLevel user

;--------------------------------
; Interface settings
!define MUI_ICON "platform\windows\installer\nodebox.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "platform\windows\installer\wizard.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP_NOSTRETCH
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "platform\windows\installer\header.bmp" ; optional
!define MUI_ABORTWARNING

;--------------------------------

; Pages
!insertmacro MUI_PAGE_LICENSE "LICENSE.txt"
!insertmacro MUI_PAGE_INSTFILES

!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT "Run NodeBox"
!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchNodeBox"
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

;--------------------------------

!insertmacro MUI_LANGUAGE "English"
BrandingText "NodeBox"

;--------------------------------

; The stuff to install
Section ""
  SetOutPath $INSTDIR
  ; Copy all application resources.
  File /r dist\windows\NodeBox\*.*
  ; Store installation folder.
  WriteRegStr HKCU "Software\NodeBox" "" $INSTDIR
  ; Create shortcuts in start menu and desktop.
  CreateShortCut $SMPROGRAMS\NodeBox.lnk $INSTDIR\NodeBox.exe
  CreateShortCut $DESKTOP\NodeBox.lnk "$INSTDIR\NodeBox.exe"
  ; Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  ; Write uninstaller to add/remove programs.
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox" "DisplayName" "NodeBox"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox" "DisplayIcon" "$INSTDIR\NodeBox.exe"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox" "Publisher" "Experimental Media Group"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox" "InstallLocation" "$INSTDIR"
SectionEnd

Section "Uninstall"
  ; Remove installation directory.
  RMDir /r "$INSTDIR"
  ; Remove NodeBox registry key.
  DeleteRegKey /ifempty HKCU "Software\NodeBox"
  ; Remove from installed programs.
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\NodeBox"
  ; Remove shortcuts from start menu and desktop.
  Delete $SMPROGRAMS\NodeBox.lnk
  Delete $DESKTOP\NodeBox.lnk
SectionEnd

Function LaunchNodeBox
  Exec '"$INSTDIR\NodeBox.exe"'
FunctionEnd