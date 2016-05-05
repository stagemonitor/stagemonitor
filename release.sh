#!/bin/sh

# Any subsequent commands which fail will cause the shell script to exit immediately
set -e

./gradlew clean release

git push

VERSION=$(git describe --tags `git rev-list --tags --max-count=1`)

# check out the tag that was just created
git checkout $VERSION

./gradlew uploadArchives

git checkout master

# build and push grafana
./gradlew :stagemonitor-grafana-elasticsearch:clean :stagemonitor-grafana-elasticsearch:build
cd stagemonitor-grafana-elasticsearch
rm -rf .git
git init
git remote add origin git@github.com:stagemonitor/stagemonitor-grafana-elasticsearch.git
git fetch
git reset --soft origin/master
git add .
git commit -m "Releasing $VERSION"
git tag -a -m "Releasing $VERSION" "$VERSION"
git push origin master --follow-tags
