"""Compile and run isolated launcher install/update regression checks. --release also checks the live release archive."""
import subprocess
import sys
from pathlib import Path
from build import JDK, OUT, HERE

subprocess.run([sys.executable, str(HERE / "build.py")], check=True)
classes = OUT / "test-classes"
classes.mkdir(parents=True, exist_ok=True)
subprocess.run([str(JDK / "bin/javac"), "--release", "21", "-encoding", "UTF-8", "-cp", str(OUT / "classes"),
                "-d", str(classes), *map(str, (HERE / "test").rglob("*.java"))], check=True)
import os
subprocess.run([str(JDK / "bin/java"), "-Djava.awt.headless=true", "-cp", os.pathsep.join(map(str, [OUT / "classes", classes])),
                "crazycraft.loader.ClientSetupTest", *sys.argv[1:]], check=True)
