.PHONY: install

jar: class
	jar cvf TZTester.jar src/tangentz/TZTester.class
class:
	javac -classpath lib/ContestApplet.jar src/tangentz/TZTester.java
install:
	cp TZTester.jar ${HOME}/topcoder/
clean:
	$(RM) TZTester.jar src/tangentz/TZTester.class