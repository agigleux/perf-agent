#!/bin/sh

export MAVEN_OPTS='-Xmx256m'
mvn clean package install -DskipTests -DdownloadSources=true -DdownloadJavadocs=true
