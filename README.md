# VoidQuit

[![License](https://img.shields.io/github/license/cbtc-59/VoidQuit)](https://choosealicense.com/licenses/mit/)
[![Modrinth](https://img.shields.io/modrinth/dt/voidquit?color=00AF5C&label=Modrinth%20downloads&logo=modrinth)](https://modrinth.com/mod/voidquit)
[![GitHub](https://img.shields.io/github/downloads/cbtc-59/VoidQuit/total?color=161616&label=GitHub%20downloads&logo=github)](https://github.com/cbtc-59/VoidQuit/releases)

玩家掉入虚空时自动断开连接/退出世界，防止死亡丢物品。

## 支持的版本

| MC 版本 | 世界底界 |
|---|---|
| 1.16.5、1.17.1 | 0 |
| 1.18.2、1.19.4 | -64 |
| 1.20.1、1.20.2、1.20.4、1.20.6 | -64 |
| 1.21.1 ~ 1.21.11 | -64 |
| 26.1.2 | -64 |

## 功能

- 检测玩家 Y 坐标，低于世界最底层指定格数后自动退出
- 支持所有游戏模式（生存/极限/冒险/创造/旁观）
- 单机模式保存退出，服务器模式断开连接
- 可配置的触发深度和冷却时间
- 退出画面支持单机/服务器场景区分（回主菜单 / 回服务器列表）

## 配置

配置文件位于 `config/voidquit.json`（首次运行自动生成）：

```json
{
  "fallDepth": 24,
  "cooldownSeconds": 5,
  "enabledSingleplayer": true,
  "enabledServer": true,
  "exitMessage": "已自动退出，防止虚空死亡"
}
```

| 字段 | 默认 | 说明 |
|------|------|------|
| `fallDepth` | 24 | 世界最底层以下多少格触发 |
| `cooldownSeconds` | 5 | 触发后冷却秒数，0 关闭 |
| `enabledSingleplayer` | true | 单机模式下是否启用 |
| `enabledServer` | true | 服务器模式下是否启用 |
| `exitMessage` | "已自动退出，防止虚空死亡" | 弹窗文字，`""` 不弹 |

## 触发阈值

实际触发 Y = `min(虚空边界 - 64, 虚空边界 - fallDepth)`。

**1.18 及更高版本**（主世界底界 -64、下界 0）：

| 维度 | 生存/极限/冒险 | 创造/旁观 |
|------|---------------|-----------|
| 主世界 | Y < -88 | Y < -128 |
| 下界 | Y < -24 | Y < -64 |
| 末地 | Y < -24 | Y < -64 |

**1.16.5 / 1.17.1**（所有维度底界 0）：

| 维度 | 生存/极限/冒险 | 创造/旁观 |
|------|---------------|-----------|
| 主世界/下界/末地 | Y < -24 | Y < -64 |

## 前置

- Minecraft（按上方支持版本列表）
- Fabric Loader ≥ 0.14.21（1.18.2+）/ ≥ 0.11.2（1.16.5~1.17.1）
- Fabric API
- Java 17+（1.16.5~1.20.4）、Java 21+（1.20.6+）

## 许可证

MIT

## 致谢 Credits

- 多版本构建基于 [Fallen-Breath/preprocessor](https://github.com/Fallen-Breath/preprocessor)（ReplayMod Preprocessor 的 fork）
- 退出机制（断网保存）方案参考 [Fallen-Breath/tweakermore](https://github.com/Fallen-Breath/tweakermore)
