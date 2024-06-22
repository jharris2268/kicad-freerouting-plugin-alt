REV=1
SRCDIR=src/main/java
OBJDIR=build/obj
JAR=freerouting_stdout.jar


include Makefile.src

freerouting_stdout.jar: build/classes.stamp
#	cp Constants.class main/java
	jar cmf build/MANIFEST.MF $(JAR) `find -name \*.class`

build/Constants.java: FORCE
	cd build && rev=$(REV) ./gen.sh

build/classes.stamp: $(SRC)
	mkdir -p $(OBJDIR)
	cp LICENSE $(OBJDIR)
	javac -d $(OBJDIR) $(SRC) -Xlint:deprecation
	touch build/classes.stamp



clean:
	rm -rf $(OBJDIR) build/classes.stamp

FORCE:
