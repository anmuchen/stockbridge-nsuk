import json
import os
import shutil
import zipfile

PROJ = r"E:/WorkyBuddy AI/2026-10-24-11-26-27/stockbridge-nsuk"
SRC = os.path.join(PROJ, "backups", "stockbridge-1.0.4-orig.jar")
OUT = os.path.join(PROJ, "dist", "stockbridge-1.0.4-nsuk.jar")
BUILD = os.path.join(PROJ, "build")

# 新增 / 替换的 class
NEW_CLASSES = {
    "com/stockbridge/colony_terminal/ColonyIntegration.class",
    "com/stockbridge/client/gui/colony_terminal/ColonyDemandRowRenderer.class",
    "com/stockbridge/client/integration/JeiIntegration.class",
    "com/stockbridge/client/integration/EmiIntegration.class",
    "com/stockbridge/integration/nsuk/NsukNeedsProvider.class",
    "com/stockbridge/integration/nsuk/NsukNeedsProvider$CachedNeeds.class",
    "com/stockbridge/integration/nsuk/NsukNeedsProvider$CitySnapshot.class",
}

ZH = {
    "block.stockbridge.colony_demand_terminal": "模拟城市需求终端",
    "item.stockbridge.colony_demand_terminal": "模拟城市需求终端",
    "block.stockbridge.wireless_colony_demand_terminal": "无线模拟城市需求终端",
    "item.stockbridge.wireless_colony_demand_terminal": "无线模拟城市需求终端",
    "stockbridge.configuration.colonyScanRadius": "城市扫描半径",
    "stockbridge.configuration.colonyRefreshTicks": "城市刷新间隔",
    "stockbridge.configuration.colonyTerminalEnergy": "终端能耗",
    "stockbridge.configuration.colonyScanRadius.tooltip": "自动检测城市的最大距离。",
    "stockbridge.configuration.colonyRefreshTicks.tooltip": "两次自动刷新城市需求之间的 tick 数。",
    "stockbridge.configuration.colonyTerminalEnergy.tooltip": "模拟城市需求终端的能量消耗（每秒 FE）。",
    "stockbridge.configuration.colonyMaxDisplayEntries.tooltip": "界面中最多显示的城市需求行数。",
    "stockbridge.configuration.wirelessTerminalRange.tooltip": "无线模拟城市需求终端连接 RS 网络的扫描范围。",
    "stockbridge.configuration.wirelessTerminalEnergy.tooltip": "无线模拟城市需求终端的能量消耗（每秒 FE）。",
    "config.jade.plugin_stockbridge.colony_demand_terminal": "StockBridge 模拟城市需求终端",
    "jade.stockbridge.colony_requires_minecolonies": "需要 New-Sim-U-Kraft",
    "jade.stockbridge.colony_name": "城市：%s",
    "gui.stockbridge.colony_demand.cycle_colony": "城市",
    "gui.stockbridge.colony_demand.colony": "城市：%s",
    "gui.stockbridge.colony_demand.no_colony": "未找到城市",
    "gui.stockbridge.colony_demand.requires_minecolonies": "安装 New-Sim-U-Kraft 后才能查看建造需求。",
    "gui.stockbridge.colony_demand.all_fulfilled": "当前城市没有待满足的建造需求。",
    "gui.stockbridge.colony_demand.selected_unavailable": "该需求已失效，请刷新后重试。",
    "gui.stockbridge.colony_demand.footer": "RS 网络 ---- New-Sim-U-Kraft",
    "gui.stockbridge.colony_demand.copy_prefix": "城市缺口：%s",
    "guide.stockbridge.chapter.intro.page1": "StockBridge 把精致存储（RS）与机械动力物流、New-Sim-U-Kraft 的模拟城市建造需求连接起来。",
    "guide.stockbridge.chapter.intro.page2": "本指南随时可合成：书 + 2 张纸 + 富石英铁。也可以使用模拟城市需求终端界面上的「指南」按钮快速查看。",
    "guide.stockbridge.chapter.colony_terminal.title": "模拟城市需求终端",
    "guide.stockbridge.chapter.colony_terminal.page1": "模拟城市需求终端读取 New-Sim-U-Kraft 城市的在建建筑剩余材料，并与 RS 存储比对。需要 New-Sim-U-Kraft 以及 RS 线缆连接。",
    "guide.stockbridge.chapter.colony_terminal.page1.note": "放在城市附近，并接入 RS 网络。",
    "guide.stockbridge.chapter.colony_terminal.page2": "各列含义：需求 = 在建建筑的剩余材料总量。RS 中 = 网络上已有的存量。缺口 = 仍然缺少的数量（需求 − RS 中）。",
    "guide.stockbridge.chapter.colony_terminal.page3": "把终端放在城市扫描半径内（配置项 colonyScanRadius）。它会自动选取最近的城市；若有多个，可用「城市」按钮循环切换。「刷新」按钮可手动更新列表。",
    "guide.stockbridge.chapter.colony_terminal.page4": "「复制缺口列表」—— 把缺少的物品发送到聊天栏。「在 RS 中合成缺口物品」—— 为每一行缺口启动 RS 自动合成（需先配置好样板和合成器）。点击某一行可在 JEI 中查看配方。",
    "guide.stockbridge.chapter.colony_terminal.page5": "该终端不会把物品送进建造箱 —— 它只是一块规划面板。要满足需求，需要通过自动合成存入 RS，或把材料导出到城市建造箱旁的容器。绿色行表示 RS 中存量已足够。",
    "guide.stockbridge.chapter.wireless_terminal.page1": "无线模拟城市需求终端与有线版界面相同，但它在一定范围内无线接入 RS 网络，方块本身不需要接线缆。",
    "guide.stockbridge.chapter.wireless_terminal.page2": "合成：模拟城市需求终端 + RS 无线发射器。放置在 wirelessTerminalRange 范围内即可，适合直接放在市政厅。",
    "guide.stockbridge.chapter.troubleshooting.page1": "未找到城市 —— 确认已安装 New-Sim-U-Kraft 并建立城市，或把接口放近城市核心，或在配置中调大 colonyScanRadius。",
}

EN = {
    "block.stockbridge.colony_demand_terminal": "SimCity Demand Terminal",
    "item.stockbridge.colony_demand_terminal": "SimCity Demand Terminal",
    "block.stockbridge.wireless_colony_demand_terminal": "Wireless SimCity Demand Terminal",
    "item.stockbridge.wireless_colony_demand_terminal": "Wireless SimCity Demand Terminal",
    "stockbridge.configuration.colonyScanRadius": "City Scan Radius",
    "stockbridge.configuration.colonyRefreshTicks": "City Refresh Interval",
    "stockbridge.configuration.colonyTerminalEnergy": "Terminal Energy",
    "stockbridge.configuration.colonyScanRadius.tooltip": "Maximum distance to auto-detect a city.",
    "stockbridge.configuration.colonyRefreshTicks.tooltip": "Ticks between automatic city demand refreshes.",
    "stockbridge.configuration.colonyTerminalEnergy.tooltip": "Energy consumption (FE per second) for the SimCity Demand Terminal.",
    "stockbridge.configuration.colonyMaxDisplayEntries.tooltip": "Maximum city demand rows shown in the GUI.",
    "stockbridge.configuration.wirelessTerminalRange.tooltip": "RS connection scan range for the Wireless SimCity Demand Terminal.",
    "stockbridge.configuration.wirelessTerminalEnergy.tooltip": "Energy consumption (FE per second) for the Wireless SimCity Demand Terminal.",
    "config.jade.plugin_stockbridge.colony_demand_terminal": "StockBridge SimCity Demand Terminal",
    "jade.stockbridge.colony_requires_minecolonies": "Requires New-Sim-U-Kraft",
    "jade.stockbridge.colony_name": "City: %s",
    "gui.stockbridge.colony_demand.cycle_colony": "City",
    "gui.stockbridge.colony_demand.colony": "City: %s",
    "gui.stockbridge.colony_demand.no_colony": "No city found",
    "gui.stockbridge.colony_demand.requires_minecolonies": "Install New-Sim-U-Kraft to view build demands.",
    "gui.stockbridge.colony_demand.all_fulfilled": "No outstanding build demands for this city.",
    "gui.stockbridge.colony_demand.selected_unavailable": "That demand is no longer available. Refresh and try again.",
    "gui.stockbridge.colony_demand.footer": "RS Network ---- New-Sim-U-Kraft",
    "gui.stockbridge.colony_demand.copy_prefix": "City deficit: %s",
    "guide.stockbridge.chapter.intro.page1": "StockBridge connects Refined Storage (RS) with Create logistics and New-Sim-U-Kraft city build demands.",
    "guide.stockbridge.chapter.intro.page2": "Craft this guide anytime: Book + 2 Paper + Quartz Enriched Iron. Use the Guide button in the SimCity Demand Terminal GUI for quick help.",
    "guide.stockbridge.chapter.colony_terminal.title": "SimCity Demand Terminal",
    "guide.stockbridge.chapter.colony_terminal.page1": "The SimCity Demand Terminal reads the remaining materials of buildings under construction in a New-Sim-U-Kraft city and compares them with RS storage. Requires New-Sim-U-Kraft and an RS cable connection.",
    "guide.stockbridge.chapter.colony_terminal.page1.note": "Place near the city and wire it into RS.",
    "guide.stockbridge.chapter.colony_terminal.page2": "Columns: Needs = total remaining materials for buildings under construction. In RS = already stored on the network. Deficit = still missing (Needs minus In RS).",
    "guide.stockbridge.chapter.colony_terminal.page3": "Place the terminal within city scan radius (config: colonyScanRadius). It auto-picks the nearest city; use the City button to cycle if you have several. Refresh updates the list manually.",
    "guide.stockbridge.chapter.colony_terminal.page4": "Copy deficit list - sends missing items to chat. Craft deficits in RS - starts RS autocrafting for every deficit row (you must set up patterns/crafters first). Click a row to open the recipe in JEI.",
    "guide.stockbridge.chapter.colony_terminal.page5": "The terminal does NOT feed items into the build box - it is a planning dashboard. Fulfill demands by autocrafting into RS, or export materials into containers next to the city build box. Green rows = RS already has enough.",
    "guide.stockbridge.chapter.wireless_terminal.page1": "The Wireless SimCity Demand Terminal shares the same GUI but links to the RS network over a configurable range, with no cable at the block itself.",
    "guide.stockbridge.chapter.wireless_terminal.page2": "Craft: SimCity Demand Terminal + RS Wireless Transmitter. Place anywhere within wirelessTerminalRange - handy at the town hall.",
    "guide.stockbridge.chapter.troubleshooting.page1": "No city found - make sure New-Sim-U-Kraft is installed and a city exists, place the interface closer to the city core, or raise colonyScanRadius in the config.",
}


def patch_lang(raw: bytes, table: dict) -> bytes:
    data = json.loads(raw.decode("utf-8"))
    changed = 0
    for key, value in table.items():
        if key in data:
            data[key] = value
            changed += 1
    print("  patched keys:", changed)
    return json.dumps(data, ensure_ascii=False, indent=2).encode("utf-8")


def main():
    if not os.path.isfile(SRC):
        raise SystemExit("source jar missing: " + SRC)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    if os.path.exists(OUT):
        os.remove(OUT)

    lang_map = {
        "assets/stockbridge/lang/zh_cn.json": ZH,
        "assets/stockbridge/lang/en_us.json": EN,
    }

    with zipfile.ZipFile(SRC) as zin, zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            name = item.filename
            if name in NEW_CLASSES:
                continue  # 稍后写入新版本
            payload = zin.read(name)
            if name in lang_map:
                print("lang:", name)
                payload = patch_lang(payload, lang_map[name])
            zout.writestr(item, payload)

        for rel in NEW_CLASSES:
            src_file = os.path.join(BUILD, rel.replace("/", os.sep))
            if not os.path.isfile(src_file):
                raise SystemExit("compiled class missing: " + src_file)
            info = zipfile.ZipInfo(rel, date_time=(2026, 10, 24, 12, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            with open(src_file, "rb") as fh:
                zout.writestr(info, fh.read())

    print("written:", OUT, os.path.getsize(OUT), "bytes")

    with zipfile.ZipFile(OUT) as z:
        bad = z.testzip()
        print("zip integrity:", "OK" if bad is None else ("BAD " + bad))
        print("entries:", len(z.namelist()))
        for rel in sorted(NEW_CLASSES):
            print("  present", rel, rel in z.namelist())


if __name__ == "__main__":
    main()
