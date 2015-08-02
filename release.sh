#!/bin/sh

# Any subsequent commands which fail will cause the shell script to exit immediately
set -e

./gradlew clean release

git push

# check out the tag that was just created
git checkout $(git describe --tags `git rev-list --tags --max-count=1`)

./gradlew uploadArchives

