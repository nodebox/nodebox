; This script creates a Java launcher for NodeBox.

Name "NodeBox"
Caption "NodeBox"
Icon "platform\windows\installer\nodebox.ico"
OutFile "dist\windows\nodebox\NodeBox.exe"
RequestExecutionLevel user

SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow

!define CLASSPATH "lib\nodebox.jar"
!define CLASS "nodebox.client.Application"

!include "FileFunc.nsh"

Section ""
  Call GetJRE
  Pop $R0

  ; change for your purpose (-jar etc.)
  ${GetParameters} $0
  StrCpy $0 '"$R0" -Xms128m -Xmx1024m -classpath "${CLASSPATH}" ${CLASS} $0'

  SetOutPath $EXEDIR
  Exec $0
SectionEnd

; Returns the full path of a valid javaw.exe.
; We assume it is under nodebox\jre\bin\javaw.exe
Function GetJRE
  Push $R0
  Push $R1

  ; Use javaw.exe to avoid DOS box.
  !define JAVAEXE "javaw.exe"

  ClearErrors
  StrCpy $R0 "$EXEDIR\jre\bin\${JAVAEXE}"
  IfFileExists $R0 JreFound
  StrCpy $R0 ""

  ; Show an error message
  MessageBox MB_OK|MB_ICONSTOP "No Java found. Please download NodeBox again from www.nodebox.net."
  StrCpy $R0 "${JAVAEXE}"  ;; As a last resort, try the current directory.

 JreFound:
  Pop $R1
  Exch $R0
FunctionEnd
