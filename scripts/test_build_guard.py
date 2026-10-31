"""The pack's builds refuse test-only pieces: OreSpawn Integrations' GameTest jar, any other *-gametests jar, and the
-Doi.gametests switch in a shipped JVM argument file.   python -I scripts/test_build_guard.py"""
import sys
import tempfile
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_packs as bp  # noqa: E402


def refused(fn, *args):
    try:
        fn(*args)
    except SystemExit as e:
        return str(e)
    return None


assert bp.test_only_problems(["orespawn_integrations-0.10.0.jar", "jei-19.jar"], {"user_jvm_args.txt": "-Xmx6G"}) == []
assert bp.test_only_problems(["orespawn_integrations_tests-0.10.0.jar"], {})
assert bp.test_only_problems(["somemod-gametests-1.0.jar"], {})
assert bp.test_only_problems([], {"user_jvm_args.txt": "-Xmx6G\n-Doi.gametests=true\n"})
assert refused(bp.refuse_test_only, "x", ["orespawn_integrations_tests-0.10.0.jar"], {})
assert refused(bp.refuse_test_only, "x", ["orespawn_integrations-0.10.0.jar"], {"a": "-Xmx6G"}) is None
with tempfile.TemporaryDirectory() as tmp:
    good, bad = Path(tmp) / "good.zip", Path(tmp) / "bad.zip"
    with zipfile.ZipFile(good, "w") as z:
        z.writestr("user_jvm_args.txt", "-Xmx6G\n")
        z.writestr("mods/orespawn_integrations-0.10.0.jar", b"PK")
    with zipfile.ZipFile(bad, "w") as z:
        z.writestr("user_jvm_args.txt", "-Xmx6G -Doi.gametests=true\n")
    assert refused(bp.check_zip_for_test_only, good) is None
    assert "oi.gametests" in refused(bp.check_zip_for_test_only, bad)
print("build guard checks passed: test jars and the -Doi.gametests switch are refused, a normal build passes")
