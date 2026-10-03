#!/usr/bin/env bash
set -u
echo "=== JARS"
find build ~/.gradle -name '*.jar' -path '*moddev*' 2>/dev/null | head -20
JAR=$(find build/moddev -name 'neoforge-*.jar' ! -name '*sources*' | head -1)
SRC=$(find build/moddev -name 'neoforge-*sources*.jar' | head -1)
echo "JAR=$JAR SRC=$SRC"
short() { sed -E 's/\b([a-z][a-z0-9_]*\.)+([A-Z])/\2/g'; }
if [ -f .probe/grep.txt ]; then
  echo "=== CLASS GREP"
  unzip -Z1 "$JAR" | grep '\.class$' | grep -v '\$[0-9]' | grep -E -f .probe/grep.txt | sed 's/\.class$//' | head -400
fi
if [ -f .probe/classes.txt ]; then
  while read -r c; do
    [ -z "$c" ] && continue
    echo "=== $c"
    javap -protected -cp "$JAR" "$c" 2>&1 | grep -v '^Compiled from' | short
  done < .probe/classes.txt
fi
if [ -f .probe/sources.txt ]; then
  while read -r f a b; do
    [ -z "$f" ] && continue
    echo "=== SRC $f $a-$b"
    unzip -p "$SRC" "$f" | sed -n "${a},${b}p"
  done < .probe/sources.txt
fi
