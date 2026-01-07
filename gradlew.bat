@if "%DEBUG%" == "" @echo off
set CLASSPATH=%~dp0gradle\wrapper\gradle-wrapper.jar
java -Dorg.gradle.appname=gradlew -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
