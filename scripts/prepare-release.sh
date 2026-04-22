#!/bin/bash
set -e

VERSION=$1

if [ -z "$VERSION" ]; then
  echo "Usage: $0 <version>"
  exit 1
fi

mkdir -p jiraminer/config
cp README.md jiraminer/README.md
cp docs/jiraminer-config-template.properties jiraminer/config/jiraminer-config.properties
cp bin/jiraminer.sh jiraminer/jiraminer.sh
cp bin/jiraminer.bat jiraminer/jiraminer.bat
chmod +x jiraminer/jiraminer.sh
cp target/jiraminer.jar jiraminer/jiraminer.jar

if [ -f "releaseNotes/v${VERSION}.md" ]; then
  cp "releaseNotes/v${VERSION}.md" jiraminer/CHANGELOG.md
fi

zip -r jiraminer.zip jiraminer
