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

CFLAGS=-classpath /usr/share/hadoop/hadoop-core-1.0.4.jar:/usr/lib/hadoop/client/\*:/usr/share/java/js.jar:.
TARGET=Eggshell.jar

all: $(TARGET)

%.jar: %.java *.java
	rm -rf classes
	mkdir classes
	javac $(CFLAGS) -d classes $<
	jar -cvf $@ -C classes .
	jar -uvf $@ eggshell.js
	jar -i $@
	rm -rf classes

clean:
	rm $(TARGET)

