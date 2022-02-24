#!/bin/sh

curl -Lo sbt https://raw.githubusercontent.com/sbt/sbt/v1.6.2/sbt

chmod +x ./sbt

./sbt buildWorkers
