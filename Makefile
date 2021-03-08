all: lettuce-600/src/main/java/com/example/Application.java lettuce-600/build.gradle

lettuce-600/src/main/java/com/example/Application.java: lettuce-534/src/main/java/com/example/Application.java
	cp $< $@

lettuce-600/build.gradle: lettuce-534/build.gradle
	cp $< $@
	perl -i -pe 's/io.lettuce:lettuce-core:5.3.4.RELEASE/io.lettuce:lettuce-core:6.0.0.RELEASE/g' $@

test: test-534 test-600

test-534:
	./gradlew :lettuce-534:shadowJar
	java -jar lettuce-534/build/libs/lettuce-534-1.0-SNAPSHOT-all.jar

test-600:
	./gradlew :lettuce-600:shadowJar
	java -jar lettuce-600/build/libs/lettuce-600-1.0-SNAPSHOT-all.jar

.PHONY: test test-534 test-600
:wq
