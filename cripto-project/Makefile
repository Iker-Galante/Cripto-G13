# Wraps Maven: `make` builds target/stegobmp.jar, which the stegobmp script runs.

JAR := target/stegobmp.jar
SOURCES := $(shell find src/main -type f)

# Folder with the course's sample images. The tests that need them are skipped if it is missing.
export STEGOBMP_SAMPLES ?= ../TP/ejemplo

.PHONY: all test clean

all: $(JAR)

$(JAR): pom.xml $(SOURCES)
	mvn -q package -Dmaven.test.skip=true
	chmod +x stegobmp
	touch $@

test:
	mvn -q test

clean:
	mvn -q clean
