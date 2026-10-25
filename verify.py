import os
import struct
import sys
import zipfile

PROJ = r"E:/WorkyBuddy AI/2026-10-24-11-26-27/stockbridge-nsuk"

CP_JARS = [
    os.path.expanduser("~") + r"/.gradle/caches/neoformruntime/intermediate_results/compiledWithNeoForge_449f0b75a1fc1fde7569ac098d8d19643c4a48a6_output.jar",
    r"E:\.minecraft\versions\新：模拟大都市：启航\mods\[新：模拟大都市] New-Sim-U-Kraft-2.2.0-neoforge-1.21.1.jar",
    os.path.expanduser("~") + r"/.gradle/caches/modules-2/files-2.1/net.neoforged.fancymodloader/loader/4.0.43/fb10b7bf2f568a9676ad8b426b19c23badbbd98a/loader-4.0.43.jar",
    r"E:\.minecraft\libraries\com\mojang\brigadier\1.3.10\brigadier-1.3.10.jar",
    r"E:\.minecraft\libraries\org\slf4j\slf4j-api\2.0.17\slf4j-api-2.0.17.jar",
    os.path.join(PROJ, "backups", "stockbridge-1.0.4-orig.jar"),
    os.path.join(PROJ, "dist", "stockbridge-1.0.4-nsuk.jar"),
]


def parse_constant_pool(data):
    """返回 (class_refs, utf8_pool)，class_refs 是内部名列表。"""
    assert data[:4] == b"\xca\xfe\xba\xbe"
    count = struct.unpack(">H", data[8:10])[0]
    pos = 10
    utf8 = {}
    class_refs = []
    i = 1
    while i < count:
        tag = data[pos]
        pos += 1
        if tag == 1:  # Utf8
            length = struct.unpack(">H", data[pos:pos + 2])[0]
            pos += 2
            utf8[i] = data[pos:pos + length].decode("utf-8", "replace")
            pos += length
        elif tag in (7, 8, 16, 19, 20):  # Class/String/MethodType/Module/Package
            idx = struct.unpack(">H", data[pos:pos + 2])[0]
            pos += 2
            if tag == 7:
                class_refs.append(idx)
        elif tag in (15,):  # MethodHandle
            pos += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            pos += 4
        elif tag in (5, 6):  # Long/Double
            pos += 8
            i += 1
        else:
            raise ValueError("unknown constant tag %d at %d" % (tag, pos - 1))
        i += 1
    names = [utf8[idx] for idx in class_refs if idx in utf8]
    return names


def build_class_index(jars):
    index = set()
    for jar in jars:
        if not os.path.isfile(jar):
            continue
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                if name.endswith(".class"):
                    index.add(name[:-6])
    return index


def main():
    index = build_class_index(CP_JARS)
    print("classpath classes indexed:", len(index))

    target_jar = os.path.join(PROJ, "dist", "stockbridge-1.0.4-nsuk.jar")
    targets = [
        "com/stockbridge/integration/nsuk/NsukNeedsProvider.class",
        "com/stockbridge/integration/nsuk/NsukNeedsProvider$CitySnapshot.class",
        "com/stockbridge/integration/nsuk/NsukNeedsProvider$CachedNeeds.class",
        "com/stockbridge/colony_terminal/ColonyIntegration.class",
        "com/stockbridge/client/gui/colony_terminal/ColonyDemandRowRenderer.class",
        "com/stockbridge/client/integration/JeiIntegration.class",
        "com/stockbridge/client/integration/EmiIntegration.class",
    ]

    unresolved_total = 0
    with zipfile.ZipFile(target_jar) as z:
        for target in targets:
            refs = parse_constant_pool(z.read(target))
            unresolved = []
            for ref in refs:
                if ref.startswith("["):
                    continue
                if ref.startswith("java/") or ref.startswith("javax/") or ref.startswith("jdk/"):
                    continue
                if ref not in index:
                    unresolved.append(ref)
            unresolved_total += len(unresolved)
            print("\n== %s : %d refs, %d unresolved" % (target, len(refs), len(unresolved)))
            for ref in sorted(set(unresolved)):
                print("   UNRESOLVED", ref)

    print("\nTOTAL UNRESOLVED:", unresolved_total)
    return 0 if unresolved_total == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
