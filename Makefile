SOURCE=$(wildcard com/drmaciver/vantagetree/*java)
CLASSES=$(SOURCE:.java=.class)

all: $(CLASSES)

clean:
	rm -rf $(CLASSES)
	rm -f fas

%.class: %.java
	javac -Xlint:unchecked $(C_FLAGS) $<
