# mclhr 设计

## 目标

1. 库文件与实例解耦：同一份 jar 被多个实例共享；
2. 可配置多个 store（全局、组、实例）；
3. 模块清单可校验（SHA-1），缺失时给出明确错误；
4. JavaFX 不再由 Agent 硬编码扫描 HMCL 缓存，而是走 mclhr 的解析器；
5. 无第三方依赖，JDK 17+ 即可运行。

## 组件

| 类 | 职责 |
|----|------|
| `MclhrLibrary` | 坐标（group/artifact/version/classifier/sha1）→ 扁平文件名 |
| `MclhrManifest` | 读取 `mclhr.json`，产出库列表 |
| `MclhrResolver` | 在多个 store 中查找、校验、返回路径 |
| `JavaFxModule` | JavaFX 坐标解析 + 旧 HMCL 缓存回退 |
| `MclhrRuntime` | 对外 API（解析 JavaFX / 清单） |
| `MclhrCli` | 命令行入口 |

## 查找顺序

```
实例 store（私有覆盖）
  → 组 store
  → 全局 store
  → 旧 HMCL 缓存（仅 JavaFX，迁移期）
```

同一坐标命中第一个存在的文件即返回；`sha1` 非空时校验失败直接抛
`MclhrException`，不静默降级。

## 与 ichor 的差异

- ichor 还负责 Mixin runtime 分层、类烘焙缓存；mclhr 只做库解析与 classpath；
- ichor 的库名带 vendor 后缀（`-lunar-0.1.5`），mclhr 用标准 Maven classifier 表达；
- mclhr 开源、可单独测试，不绑定任何启动器。

## 后续

- [ ] store 索引缓存（避免每次全目录扫描）
- [ ] 并行 SHA-1 校验
- [ ] `mclhr.json` 的版本/依赖约束
- [ ] 与 FLFHMCLauncher 的下载器对接（缺失库自动补齐）
