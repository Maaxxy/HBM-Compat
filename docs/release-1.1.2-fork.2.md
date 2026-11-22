# HBM Compat 1.1.2-fork.2

本版为本 fork 的发布版本，基于上游 1.1.2。

## 新增

- **模块化砧**：放入对应等级的 HBM 砧，即可按处理样板执行 HBM 的建造、回收及普通锻造配方。机器无需供电，可接入 ME 网络，并支持最多五张加速卡。
  - 支持相邻 **ME HBM 样板适配器**（配合自动提取卡回收产物）、相邻 **ME 接口**（产物返回投料接口），或在砧自身样板槽放入样板独立使用。
  - 六面均可投料和提取产物；ME 样板只填必定产出的物品，概率副产物仍按 HBM 概率产出。
- **NEI 概率产物编码**：砧的配方此前因含概率产物被拒绝编码，现只提取必定产出的产物，可直接点击“+”编码处理样板。

## 前置模组

| 模组 | 支持范围（含两端） |
| --- | --- |
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614 |
| HBM Nuclear Tech Mod | 1.0.27_X5758 ～ 1.0.27_X5808 |
| Applied Energistics 2 Unofficial | rv3-beta-1024-GTNH ～ rv3-beta-1068-GTNH |
| AE2 Fluid Crafting Rework | 1.5.99-gtnh ～ 1.5.110-gtnh |
| NotEnoughEnergistics | 1.7.38 ～ 1.7.42 |
| NotEnoughItems GTNH | 2.8.114-GTNH ～ 2.8.144-GTNH |

客户端与服务端均需安装。