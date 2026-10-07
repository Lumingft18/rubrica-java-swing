#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

rm -rf bin build
mkdir -p bin build/jar

find src -name '*.java' > build/sources.txt
# --release 8: il jar parte anche su chi ha Java 8, 11, 17
javac --release 8 -Xlint:-options -encoding UTF-8 -d bin @build/sources.txt

# il driver entra nel jar, così il doppio click funziona anche in modalità database
cp -R bin/. build/jar/
(
  cd build/jar
  jar xf ../../lib/mysql-connector-j-8.0.33.jar
  rm -f META-INF/*.SF META-INF/*.RSA META-INF/*.DSA
  jar cfe ../../Rubrica.jar rubrica.RubricaMain .
)

rm -rf dist
mkdir -p dist/pacchetto
cp Rubrica.jar schema_database.sql credenziali_database.properties README.md dist/pacchetto/
(
  cd dist/pacchetto
  zip -q -r ../Rubrica.zip .
)

# sorgenti, librerie e prove, senza dati locali o cartelle di compilazione
zip -q -r dist/Rubrica-sorgenti.zip src test lib .project .classpath .settings .gitignore \
  compila.sh test.sh README.md schema_database.sql credenziali_database.properties \
  -x '*/.*' '*/__MACOSX/*'

echo "OK: Rubrica.jar, dist/Rubrica.zip e dist/Rubrica-sorgenti.zip"
