# HBM Compat（本 fork）

Minecraft 1.7.10 的 HBM 与 AE2 兼容模组。

本仓库是 [Lyarion/HBM-Compat](https://github.com/Lyarion/HBM-Compat) 的 fork，仅记录并说明本 fork 相对上游新增的内容。上游已有功能（样板适配器、流体总线、容器罐装、NEI 配方转移等）请参阅上游仓库的说明。

## 本 fork 新增

### 模块化砧

装入对应等级的 HBM 砧，便可按处理样板执行 HBM 的建造、回收及普通锻造配方。机器本身无需供电，可连接 ME 线缆，也可装入最多五张加速卡。

- **ME HBM 样板适配器**：将处理样板放进相邻适配器，并从砧的产物格提取产物。也可在适配器中装入自动提取卡。
- **ME 接口**：将处理样板放进相邻 ME 接口，并留空砧的样板槽。产物会返回投料的接口。
- **独立使用**：将处理样板放进砧的样板槽。六面均可投料和提取产物。手动编码仅有概率产物的配方时，须在样板中填写所有可能产物。

ME 自动合成的样板只填写必定产出的物品和数量。配方的概率副产物仍会按 HBM 的概率产出。

### 拼音搜索

支持 **Not Enough Characters**（可选，仅客户端）。安装后，配方选择器（装配机、大型装配厂、化工厂、大型化工厂、电弧焊机、矿物酸化器等）、砧、压弹机、流体识别码、传送器链接器、气动存储访问终端及结构加载的搜索框支持全拼、首字母及混拼检索。拼音匹配依赖中文界面；未安装时行为与原生 HBM 一致。

安装 lwjgl3ify 时，拼音相关的 Mixin 改在 late 阶段注册，并仅在客户端生效，服务端不加载。

## 环境要求

Minecraft **1.7.10**，Forge **10.13.4.1614**。支持以下前置版本：

| 模组 | 支持范围（含两端） |
| --- | --- |
| HBM Nuclear Tech Mod | 1.0.27_X5758 ～ 1.0.27_X5808 |
| Applied Energistics 2 Unofficial | rv3-beta-1024-GTNH ～ rv3-beta-1068-GTNH |
| AE2 Fluid Crafting Rework | 1.5.99-gtnh ～ 1.5.110-gtnh |
| NotEnoughEnergistics | 1.7.38 ～ 1.7.42 |
| NotEnoughItems GTNH | 2.8.114-GTNH ～ 2.8.144-GTNH |

范围内版本未逐一测试，更高版本兼容性待确认。

客户端与服务端均需安装。升级时替换旧版 HBM Compat，不要同时保留多个版本。

可选：Not Enough Characters（仅客户端，用于拼音搜索）。

## 排查问题

输入 `/hbmc debug` 开关聊天诊断，无需 OP；消息仅自己可见，退出游戏后关闭。

## 许可与署名

**GNU 通用公共许可证 v3.0（GPLv3）**

- 流体图标与色块直接读取 HBM 自带资源，详见 [`HBM-Nuclear-Tech-TEXTURES-NOTICE.txt`](src/main/resources/META-INF/licenses/hbmcompat/HBM-Nuclear-Tech-TEXTURES-NOTICE.txt)。
- Forge 流体命名方案参照 Justus0405 的 NTM Fluid Converters。
- 感谢 GTNewHorizons、HBM NTM 与上游作者 Lyarion。