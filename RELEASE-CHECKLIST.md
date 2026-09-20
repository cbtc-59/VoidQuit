# 发版前检查清单

每次发版 vX.Y.Z 按顺序过一遍，全部通过才发。发版动作也可交给 CI（推送 `v*` 标签自动执行），手动发版时按下表操作。

## 0. 准备

- [ ] 更新 `gradle.properties` 的 `mod_version` 为目标版本
- [ ] 更新仓库根 `RELEASE-NOTES.md` 为本次变更日志（CI 发版会用它做 GitHub Release 描述和 Modrinth changelog）
- [ ] 确认 `git status` 干净，已提交全部改动

## 1. 全量构建

```bash
./gradlew buildAndGather
```

- [ ] 全部 26 个节点构建成功，零报错
- [ ] `build/libs/` 产物数量正确（26 个发行 jar，名字以 `-dev` 结尾属正常——日常构建不带时间戳）

## 2. 抽样冒烟测试（runClient）

必测节点（退出流程断代差异最大处）：

- [ ] 主项目 26.1.2-Fabric 与 26.1.2-neoforge
- [ ] 1.16.5（最低版本：TextComponent、老退出路径、v1 命令注册）
- [ ] 1.21.11（disconnectWithProgressScreen 路径、v2 命令注册）
- [ ] 1.20.4-neoforge（NeoForge 老事件模型）

每节点检查项：

- [ ] 游戏内执行 `/voidquit reload` 出现"配置已重载"，手改 `fallDepth` 后触发线随之变化
- [ ] 生存模式跳虚空：Y 低于触发线后自动退出，单人回主菜单
- [ ] 退出前世界已保存（重进世界位置/物品正常）
- [ ] 断开画面显示 exitMessage 文案，按钮指向正确（单机"回主菜单"/多人"回服务器列表"）
- [ ] 触发后冷却期内重进世界不会立即再次触发
- [ ] 创造模式触发线更深（边界下 64 格）
- [ ] 已死亡状态不触发
- [ ] 手编配置写 `"exitMessage": null` 和负数后 reload，配置回写为默认值（自愈）
- [ ] 日志出现 `[VoidQuit] 触发虚空退出`，无异常堆栈
- [ ] F3 界面无残留（1.21.9+ 节点）

## 3. 正式发版

### 方式 A：CI 发版（推荐）

- [ ] 确认第 0~2 步全部通过
- [ ] 提交并推送，打标签 `git tag vX.Y.Z && git push origin vX.Y.Z`
- [ ] 到 GitHub Actions 页面盯"发版"工作流：全量构建 → GitHub Release → Modrinth
- [ ] 首次使用需在仓库 Settings → Secrets 配置 `MODRINTH_TOKEN`（Modrinth 后台生成，范围建议只勾版本上传）

### 方式 B：手动发版

```bash
# Git Bash 下时间戳自动取当前时间
./gradlew buildAndGather -Prelease -PbuildTimestamp=$(date +%y%m%d%H%M)
```

- [ ] 检查产物文件名带正确时间戳（缺 `-PbuildTimestamp` 会直接报错拒构，不会产出错误命名 jar）
- [ ] `git tag vX.Y.Z` 指向发版提交并推送
- [ ] GitHub Release：标题与 tag 同名，上传全部发行 jar（不含 sources），描述粘贴 RELEASE-NOTES.md
- [ ] Modrinth 发布：`CHANGELOG_PATH=RELEASE-NOTES.md MODRINTH_TOKEN=... python .github/scripts/publish_modrinth.py`（先 `--dry` 干跑核对 26 条）
- [ ] 核对 Modrinth 各条目的 game_versions 与节点 properties 一致

## 4. 收尾

- [ ] GitHub Release 与 Modrinth 的变更日志一致
- [ ] Modrinth 页面下载正常（抽查一个 jar）
