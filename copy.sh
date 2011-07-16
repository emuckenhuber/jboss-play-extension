#!/bin/sh

PLAY=/home/emuckenh/Downloads/play-1.2.2
JBOSS=/home/emuckenh/Downloads/jboss-as-web-7.0.0.Final

MODULE=$JBOSS/modules/org/jboss/extension/play/main

mkdir -p $MODULE

cp src/main/resources/module/main/module.xml $MODULE

cp target/play-extension.jar $MODULE/play-extension.jar
cp target/play-jboss-plugin.jar $PLAY/framework/play-jboss-plugin.jar

