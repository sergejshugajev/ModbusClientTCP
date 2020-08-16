@echo Compile an executable JAR file.
@call mvn compile assembly:single

@echo Run in the Java machine.
@java -jar TestModbus.jar

@pause