CLASSPATH = ../lib/snakeyaml-1.11.jar

all:
	javac -cp $(CLASSPATH) *.java
	cp sample_config.yml ../bin/
	mv *.class ../bin/
clean:
	rm -rf ../bin/*
