@echo off

set MAVEN_OPTS="-Xmx256m"
call mvn eclipse:clean eclipse:eclipse clean package install -DskipTests -DdownloadSources=true -DdownloadJavadocs=true
