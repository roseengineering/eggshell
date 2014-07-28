#
# This file is part of Eggshell.
# Copyright 2013 George Magiros
#
# Eggshell is free software: you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by the
# Free Software Foundation, either version 3 of the License, or (at
# your option) any later version.
#
# Eggshell is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
# or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
# License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with Eggshell.  If not, see <http://www.gnu.org/licenses/>.

# CFLAGS=-classpath "/opt/mahout/*:/usr/share/java/js.jar:."
CFLAGS=-classpath "/usr/share/java/js.jar:/opt/hadoop/share/hadoop/mapreduce/*:/opt/hadoop/share/hadoop/mapreduce/lib/*:/opt/hadoop/share/hadoop/common/*:."
TARGET=Eggshell.jar
JAVAC=/usr/lib/jvm/java-6-openjdk-amd64/bin/javac

all: $(TARGET)

%.jar: %.java *.java
	rm -rf classes
	mkdir classes
	$(JAVAC) $(CFLAGS) -d classes $<
	jar -cvf $@ -C classes .
	jar -uvf $@ eggshell.js
	jar -i $@
	rm -rf classes

clean:
	rm $(TARGET)

