; NodeBox installer script for use with NSIS (Nullsoft Scriptable Install System).

;--------------------------------
; Includes
!include "MUI2.nsh"
!include "FileAssociation.nsh"

;--------------------------------
; Application metadata
!define APPNAME "NodeBox"
!ifndef APPVERSION
  !define APPVERSION "snapshot"
!endif
!define APPNAMEANDVERSION "${APPNAME} ${APPVERSION}"
!define APPWEBSITE "http://nodebox.net/"

;--------------------------------
; General
Name "${APPNAME}"
OutFile "dist\nodebox-${APPVERSION}-setup.exe"
InstallDir "$PROGRAMFILES\${APPNAME}"
InstallDirRegKey HKCU "Software\${APPNAME}" ""
RequestExecutionLevel admin

;--------------------------------
; Compression
SetCompress Auto
SetCompressor /SOLID lzma
SetCompressorDictSize 32
SetDatablockOptimize On

;--------------------------------
; Interface settings
!define MUI_ICON "platform\windows\installer\installer.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "platform\windows\installer\wizard.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP_NOSTRETCH
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "platform\windows\installer\header.bmp"
!define MUI_ABORTWARNING

;--------------------------------
; Pages
!insertmacro MUI_PAGE_LICENSE "LICENSE.txt"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES

!define MUI_FINISHPAGE_LINK "Visit the NodeBox site for documentation and support"
!define MUI_FINISHPAGE_LINK_LOCATION "http://nodebox.net/"

!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT "Run ${APPNAME}"
!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchNodeBox"
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

;--------------------------------
; Language and Branding
!insertmacro MUI_LANGUAGE "English"
BrandingText "${APPNAMEANDVERSION}"

;--------------------------------
; Sections
Section ""
  SetOutPath $INSTDIR
  ; Copy all application resources.
  File /r dist\windows\NodeBox\*.*
  ; Store installation folder.
  WriteRegStr HKCU "Software\${APPNAME}" "" $INSTDIR
  ; Create shortcuts in start menu and desktop.
  CreateShortCut $SMPROGRAMS\${APPNAME}.lnk $INSTDIR\NodeBox.exe
  CreateShortCut $DESKTOP\${APPNAME}.lnk "$INSTDIR\NodeBox.exe"
  ; Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  ; Write uninstaller to add/remove programs.
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayIcon" "$INSTDIR\NodeBox.exe"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "Publisher" "Experimental Media Research Group"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "InstallLocation" "$INSTDIR"
  ;Register file extension.
  ${registerExtension} "$INSTDIR\NodeBox.exe" ".ndbx" "NDBX_File"
SectionEnd

Section "Uninstall"
  ; Unregister file extension.
  ${unregisterExtension} ".ndbx" "NDBX File"
  ; Remove installation directory.
  RMDir /r "$INSTDIR"
  ; Remove application data.
  RMDir /r "$LOCALAPPDATA\NodeBox"
  ; Remove NodeBox registry key.
  DeleteRegKey /ifempty HKCU "Software\${APPNAME}"
  ; Remove from installed programs.
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
  ; Remove shortcuts from start menu and desktop.
  Delete $SMPROGRAMS\${APPNAME}.lnk
  Delete $DESKTOP\${APPNAME}.lnk
SectionEnd

;--------------------------------
; Functions
Function LaunchNodeBox
  Exec '"$INSTDIR\NodeBox.exe"'
FunctionEnd
