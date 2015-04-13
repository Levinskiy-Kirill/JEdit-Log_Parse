#!/bin/sh
CLASSPATH=
CLASSPATH=`find jars -name '*.jar' -printf '%p:'`$CLASSPATH
java -cp "$CLASSPATH:jedit.jar" org.gjt.sp.jedit.jEdit&
