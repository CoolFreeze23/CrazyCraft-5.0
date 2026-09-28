"""Build build/loader/crazycraft-loader.jar (JDK 21, no other dependencies).

  python loader/build.py              compile and package, with pack/manifest.json bundled
  python loader/build.py --art        also refresh the images in loader/res from the instance's title screen
"""
import os
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
OUT = ROOT / "build" / "loader"
RES = HERE / "res" / "crazycraft" / "loader"
JDK = Path(os.environ.get("JAVA_HOME", r"C:\Users\alvin\AppData\Roaming\PrismLauncher\java\java-runtime-delta"))
ART = Path(r"C:\Users\alvin\AppData\Roaming\PrismLauncher\instances\CrazyCraft 5.0\minecraft\config\fancymenu\assets\orespawn")


def refresh_art():
    from PIL import Image
    RES.mkdir(parents=True, exist_ok=True)
    Image.open(ART / "keyart.png").convert("RGB").resize((1280, 720), Image.LANCZOS).save(RES / "keyart.jpg", quality=86)
    shutil.copy2(ART / "logo.png", RES / "logo.png")
    shutil.copy2(ART / "button_wide_normal.png", RES / "button.png")
    shutil.copy2(ART / "button_wide_hover.png", RES / "button_hover.png")
    logo = Image.open(ART / "logo.png").convert("RGBA")
    badge = logo.crop((1020, 0, 1180, 112))
    bbox = badge.getbbox()
    badge = badge.crop(bbox)
    side = max(badge.size)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(badge, ((side - badge.width) // 2, (side - badge.height) // 2))
    for n in (16, 32, 64):
        square.resize((n, n), Image.NEAREST if n >= 64 else Image.LANCZOS).save(RES / f"icon{n}.png")
    print("art refreshed in", RES)


def main():
    if "--art" in sys.argv or not (RES / "logo.png").exists():
        refresh_art()
    classes = OUT / "classes"
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir(parents=True)
    sources = [str(p) for p in (HERE / "src").rglob("*.java")]
    subprocess.run([str(JDK / "bin" / "javac"), "--release", "21", "-encoding", "UTF-8", "-Xlint:-options",
                    "-d", str(classes)] + sources, check=True)
    shutil.copytree(HERE / "res", classes, dirs_exist_ok=True)
    manifest = ROOT / "pack" / "manifest.json"
    if manifest.exists():
        shutil.copy2(manifest, classes / "crazycraft" / "loader" / "manifest.json")
    jar = OUT / "crazycraft-loader.jar"
    if jar.exists():
        jar.unlink()
    subprocess.run([str(JDK / "bin" / "jar"), "--create", "--file", str(jar), "--main-class", "crazycraft.loader.Main",
                    "-C", str(classes), "."], check=True)
    print(f"built {jar} ({jar.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
