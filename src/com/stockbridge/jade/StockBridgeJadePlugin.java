package com.stockbridge.jade;

import com.stockbridge.colony_terminal.ColonyDemandTerminalBlock;
import com.stockbridge.network_interface.NetworkInterfaceBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade 插件入口（修复版）。
 *
 * <p>原版 1.0.4 把同一个 {@link ColonyDemandJadeProvider} 实例分别注册给了
 * {@code ColonyDemandTerminalBlock} 和 {@code WirelessColonyDemandTerminalBlock}。
 * 由于后者继承自前者，而 Jade 的 {@code HierarchyLookup} 按类型层级匹配
 * （内部使用 {@code Class#isAssignableFrom}），父类的那次注册已经覆盖了子类，
 * 因此无线终端会命中两次同一个 provider，tooltip 中的状态行重复出现两遍。
 *
 * <p>修复方式是只保留父类的一次注册。
 */
@WailaPlugin
public class StockBridgeJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 本模组的 tooltip 全部在客户端计算，无需服务端数据同步。
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(NetworkInterfaceJadeProvider.INSTANCE, NetworkInterfaceBlock.class);
        registration.registerBlockComponent(ColonyDemandJadeProvider.INSTANCE, ColonyDemandTerminalBlock.class);
    }
}
