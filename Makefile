SOURCE=$(wildcard com/drmaciver/*java)
CLASSES=$(SOURCE:.java=.class)

all: $(CLASSES)

clean:
	rm -rf $(CLASSES)
	rm -f fas

%.class: %.java
	javac $(C_FLAGS) $<
