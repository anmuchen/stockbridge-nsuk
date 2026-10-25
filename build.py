"""一键重建 stockbridge-1.0.4-nsuk.jar。

用法：python build.py
流程：javac 编译改动类 -> pack.py 重新打包 -> verify.py 校验类链接。
"""

import os
import subprocess
import sys

PROJ = os.path.dirname(os.path.abspath(__file__))
JAVA_HOME = r"C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot"

NEARO = os.path.expanduser("~") + r"/.gradle/caches/neoformruntime/intermediate_results/compiledWithNeoForge_449f0b75a1fc1fde7569ac098d8d19643c4a48a6_output.jar"
NSK = r"E:\.minecraft\versions\新：模拟大都市：启航\mods\[新：模拟大都市] New-Sim-U-Kraft-2.2.0-neoforge-1.21.1.jar"
FML = os.path.expanduser("~") + r"/.gradle/caches/modules-2/files-2.1/net.neoforged.fancymodloader/loader/4.0.43/fb10b7bf2f568a9676ad8b426b19c23badbbd98a/loader-4.0.43.jar"
BRIG = r"E:\.minecraft\libraries\com\mojang\brigadier\1.3.10\brigadier-1.3.10.jar"
SLF4J = r"E:\.minecraft\libraries\org\slf4j\slf4j-api\2.0.17\slf4j-api-2.0.17.jar"
JADE = r"E:/WorkyBuddy AI/2026-10-24-11-26-27/tools/jade-15.10.5.jar"
ORIG = os.path.join(PROJ, "backups", "stockbridge-1.0.4-orig.jar")

SOURCES = [
    "src/com/stockbridge/integration/nsuk/NsukNeedsProvider.java",
    "src/com/stockbridge/colony_terminal/ColonyIntegration.java",
    "src/com/stockbridge/client/gui/colony_terminal/ColonyDemandRowRenderer.java",
    "src/com/stockbridge/client/integration/JeiIntegration.java",
    "src/com/stockbridge/client/integration/EmiIntegration.java",
    "src/com/stockbridge/jade/StockBridgeJadePlugin.java",
]


def run(cmd, **kw):
    print("$", " ".join(cmd) if isinstance(cmd, list) else cmd)
    return subprocess.run(cmd, **kw)


def main():
    for path in (NEARO, NSK, FML, BRIG, SLF4J, JADE, ORIG):
        if not os.path.isfile(path):
            raise SystemExit("missing dependency: " + path)

    classpath = ";".join([NEARO, NSK, FML, BRIG, SLF4J, JADE, ORIG])
    build_dir = os.path.join(PROJ, "build")
    if os.path.isdir(build_dir):
        import shutil
        shutil.rmtree(build_dir)
    os.makedirs(build_dir, exist_ok=True)

    result = run([
        os.path.join(JAVA_HOME, "bin", "javac.exe"),
        "-encoding", "UTF-8", "-source", "21", "-target", "21", "-nowarn",
        "-cp", classpath, "-d", build_dir,
    ] + [os.path.join(PROJ, s) for s in SOURCES], cwd=PROJ)
    if result.returncode != 0:
        raise SystemExit("javac failed")
    print("compile: OK")

    for script in ("pack.py", "verify.py"):
        result = run([sys.executable, os.path.join(PROJ, script)], cwd=PROJ)
        if result.returncode != 0:
            raise SystemExit(script + " failed")

    print("\nbuild finished -> dist/stockbridge-1.0.4-nsuk.jar")


if __name__ == "__main__":
    main()
