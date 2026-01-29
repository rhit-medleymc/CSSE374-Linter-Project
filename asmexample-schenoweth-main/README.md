# ASMExample-Maven

# Dependencies
Students: please study the pom.xml file to understand what dependencies (and their versions) are specified for this project.

# How to Run

The easiest way I found to correctly compile this to to build a jar.  So build it like this:

mvn package

And then run the jar like this:

java -cp target/LinterProject-1.0-rc3-jar-with-dependencies.jar example.MyFirstLinter java.lang.String java.util.ArrayList

Or like this if you wanna analyze your own local files (which you'll probably want to do to write test cases)

java -cp target/LinterProject-1.0-rc3-jar-with-dependencies.jar example.MyFirstLinter example.MyFirstLinter
