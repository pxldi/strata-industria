#!/usr/bin/env bash
set -u
JAR=$(ls build/moddev/artifacts/minecraft-patched-*.jar | grep -v sources | head -1)
unzip -Z1 "$JAR" | grep '\.class$' | grep -v '\$[0-9]' | grep -E "^($1)" | sed 's/\.class$//; s|/|.|g' > /tmp/classes.txt
wc -l /tmp/classes.txt
echo "=== BEGIN DUMP"
xargs -n 200 javap -protected -cp "$JAR" < /tmp/classes.txt 2>&1 | grep -v '^Compiled from' | sed -E 's/\b([a-z][a-z0-9_]*\.)+([A-Z])/\2/g'
echo "=== END DUMP"
