#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

# ricompilo sempre: niente prove su classi vecchie
rm -rf bin bin-test
mkdir -p build bin bin-test
find src -name '*.java' > build/sources.txt
javac --release 8 -Xlint:-options -encoding UTF-8 -d bin @build/sources.txt

find test -name '*.java' > build/test-sources.txt
javac --release 8 -encoding UTF-8 -cp "bin:lib/junit-platform-console-standalone-1.10.3.jar" -d bin-test @build/test-sources.txt
java -jar lib/junit-platform-console-standalone-1.10.3.jar execute -cp "bin:bin-test:lib/mysql-connector-j-8.0.33.jar" --scan-class-path --disable-banner --details=tree
