"""离线复刻 NSK 的 requiresMaterial 判定，用真实配置验证需求口径。

判定规则（来自 WorkMaterialPolicy.requiresMaterial 字节码）：
1. 空气/空 id -> false
2. 创造模式 -> 全部 false
3. 专家模式 -> 不在 expertSkipList 里就为 true
4. 普通/建造模式：
   - 在 basicMaterials 里 -> true
   - 开启了通类匹配，且命中某个组的「匹配材料」-> true
   - 否则 -> false
"""
import tomllib

CONFIG = r"E:\.minecraft\versions\新：模拟大都市：启航\config\simukraft-common.toml"

data = tomllib.load(open(CONFIG, "rb"))
mat = data["materials"]
basic = set(mat.get("basicMaterials", []))
groups_raw = mat.get("materialCategoryGroups", [])
matching_enabled = mat.get("enableMaterialCategoryMatching", True)
expert = data.get("general", {}).get("enableExpertMode", False)
creative = data.get("general", {}).get("enableCreativeMode", False)


def split_ids(text):
    return [x.strip() for x in text.split(",") if x.strip()]


groups = []
for raw in groups_raw:
    parts = raw.split("|")
    name = parts[0].strip()
    match_part = parts[1] if len(parts) > 1 else ""
    input_part = parts[2] if len(parts) > 2 else ""
    groups.append({
        "name": name,
        "match": split_ids(match_part),
        "inputs": split_ids(input_part),
        "raw": raw,
    })


def find_group(block_id):
    # 与 MaterialGroupInfo.contains 一致：headers 或 members 命中即算匹配
    for g in groups:
        if block_id in g["match"] or block_id in g["inputs"]:
            return g
    return None


def requires(block_id):
    if creative:
        return False, "创造模式不消耗"
    if expert:
        return True, "专家模式"
    if block_id in basic:
        return True, "基础材料列表"
    if matching_enabled:
        g = find_group(block_id)
        if g:
            return True, "通类组 " + g["name"]
    return False, "既不在基础材料也不在任何通类组"


MUST_KEEP = [
    ("minecraft:oak_stairs", "橡木楼梯"),
    ("minecraft:polished_diorite_slab", "磨制闪长岩台阶"),
    ("minecraft:oak_fence", "橡木栅栏"),
]

MUST_DROP = [
    ("minecraft:lantern", "灯笼"),
    ("simukraft:residential_control_box", "住宅控制箱"),
    ("minecraft:poppy", "虞美人"),
    ("minecraft:oak_leaves", "橡树树叶"),
    ("minecraft:red_bed", "红色床"),
    ("minecraft:torch", "火把"),
    ("minecraft:bookshelf", "书架"),
    ("minecraft:cauldron", "炼药锅"),
    ("minecraft:moss_carpet", "覆地苔藓"),
    ("minecraft:petrified_oak_slab", "石化橡木台阶"),
    ("minecraft:campfire", "营火"),
    ("minecraft:crafting_table", "橡木工作台"),
    ("minecraft:oak_pressure_plate", "橡木压力板"),
    ("minecraft:oak_sign", "橡木告示牌"),
]

print("配置：创造模式=%s 专家模式=%s 通类匹配=%s" % (creative, expert, matching_enabled))
print("基础材料 %d 项，通类组 %d 组" % (len(basic), len(groups)))
print()

print("=== 应当保留（楼梯/台阶/栅栏） ===")
ok = 0
for bid, cn in MUST_KEEP:
    need, why = requires(bid)
    mark = "OK " if need else "!! "
    if need:
        ok += 1
    print("  %s%-36s %-10s -> %s (%s)" % (mark, bid, cn, "需要" if need else "不需要", why))

print()
print("=== 应当过滤（装饰/功能方块） ===")
ok2 = 0
for bid, cn in MUST_DROP:
    need, why = requires(bid)
    mark = "OK " if not need else "!! "
    if not need:
        ok2 += 1
    print("  %s%-36s %-10s -> %s (%s)" % (mark, bid, cn, "需要" if need else "不需要", why))

print()
print("结果：保留项 %d/%d 正确，过滤项 %d/%d 正确" % (ok, len(MUST_KEEP), ok2, len(MUST_DROP)))

print()
print("=== 23 个通类组概览 ===")
for g in groups:
    print("  %-24s 匹配=%-2d 输入=%-2d  例: %s" % (
        g["name"], len(g["match"]), len(g["inputs"]),
        (g["match"][:2] if g["match"] else g["inputs"][:2])))
