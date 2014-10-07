#!/bin/sh

export MAVEN_OPTS='-Xmx256m'
mvn eclipse:clean eclipse:eclipse clean package install -DskipTests -DdownloadSources=true -DdownloadJavadocs=true
