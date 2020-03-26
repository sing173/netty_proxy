#!/bin/bash

CURDIR=$(dirname $0)
cd $CURDIR

nohup java \
-server \
-Xmx2048m \
-Xms2048m \
-XX:+PrintGCDetails \
-XX:+PrintGCTimeStamps \
-classpath ../conf:../lib/*   \
ServerStart > ../logs/est-server.log &
