# mclhr 设计

## 定位

通用、无框架依赖的 JVM 库解析运行时：

```
坐标 (group:artifact:version[:classifier])  +  多个 store  →  jar 路径 / classpath
```

不负责：下载、版本约束求解、Mixin、模块热加载。这些由使用方组合。

## 组件

| 类 | 职责 |
|----|------|
| `MclhrLibrary` | 坐标 → 扁平文件名；`parse(...)` 支持字符串传参 |
| `MclhrModule` | 清单中的模块（id + 库列表 + optional） |
| `MclhrManifest` | 解析 `mclhr.json`；兼容顶层 `libraries` |
| `MclhrResolver` | 按 store 优先级查找、SHA-1 校验、缺失策略 |
| `Mclhr` | 门面：Builder、`resolveCoordinates` / `resolveManifest` / `classpath` |
| `MclhrRuntime` | 兼容旧调用方的静态入口（deprecated） |
| `ext.javafx.JavaFxSupport` | 可选扩展：JavaFX 解析 + 旧 HMCL 缓存回退 |
| `MclhrCli` | 命令行：resolve / classpath / javafx / version |

## 查找与优先级

```
store[0]（实例私有） → store[1]（组） → store[N]（全局）
```

- 第一个存在的文件即命中；
- `sha1` 非空时校验失败直接抛异常；
- `optional=true` 或 `--allow-missing` 时跳过缺失。

## 设计选择

1. **扁平命名**：避免多级目录与坐标映射；与 Lunar ichor 的 `ichormodule-libs` 一致。
2. **核心/扩展分离**：JavaFX、Fabric 等属于使用方领域，放 `ext` 包，核心不引入依赖。
3. **只解析不加载**：classpath 如何注入（`Instrumentation`、自定义 ClassLoader、命令行）
   由宿主决定，mclhr 不绑死加载策略。
4. **坐标传参**：命令行与 API 都支持直接传坐标，适合脚本化启动器。

## 与 ichor 的差异

- ichor 是 Lunar 闭源的类加载/模块运行时，含 Mixin 分层与类烘焙；
- mclhr 是开源、通用的「库解析 + classpath」层，可与任意加载器组合。

## 后续

- [ ] store 索引缓存（避免每次全目录扫描）
- [ ] 并行 SHA-1 校验
- [ ] 版本约束 / 冲突检测
- [ ] `mclhr.json` 的 schema 校验与错误定位
- [ ] 可选下载器接口（缺失库补全）
