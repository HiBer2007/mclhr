# mclhr — multiver class loader handle runtime

> 通用的 JVM 库解析 / 共享库仓 / classpath 组装运行时。
> 名称：**m**ulti**v**er **c**lass **l**oader **h**andle **r**untime（mclhr）。
> 协议：GPL-3.0（见 `LICENSE`）。

mclhr 不绑定任何启动器、游戏或框架：它只做一件事——
**把「库坐标」解析成文件路径，并组装 classpath**。

适合：

- 多实例启动器（多个实例共用同一份库）；
- 带共享缓存/私有覆盖的 Java 应用；
- HMCL、Lunar 类启动器的可复用基础设施；
- 任何需要「按 Maven 坐标在多个目录里找 jar」的场景。

## 特性

- **多 store**：按优先级从多个目录查找，实例私有可覆盖全局共享；
- **坐标传参**：API 直接传 `group:artifact:version[:classifier]`，CLI 用 `--library`；
- **清单**：`mclhr.json` 支持 modules / libraries，含 SHA-1 与 optional；
- **classpath**：直接输出平台分隔符拼接的 classpath；
- **可扩展**：JavaFX 等框架支持放在 `ext` 包，核心零框架依赖；
- **零第三方依赖**，JDK 17+。

## Java API

```java
Mclhr mclhr = Mclhr.builder()
        .store(Path.of("instance-libraries"))   // 优先级最高
        .store(Path.of("shared-libraries"))
        .build();

// 方式 1：坐标字符串
List<Path> jars = mclhr.resolveCoordinates(List.of(
        "org.example:engine:1.0",
        "org.openjfx:javafx-base:17.0.13:win"));

// 方式 2：清单
List<Path> fromManifest = mclhr.resolve(MclhrManifest.parse(json));

// 方式 3：直接构造库对象
Path engine = mclhr.resolveCoordinate("org.example:engine:1.0");

// classpath
String cp = mclhr.classpath(MclhrManifest.parse(json));
```

坐标格式：`group:artifact:version[:classifier][@sha1][?optional]`。

## CLI

```bash
# 传坐标
java -jar mclhr.jar resolve --library org.example:engine:1.0 --store ./libraries
java -jar mclhr.jar classpath --library org.example:engine:1.0 --store ./libraries

# 传清单
java -jar mclhr.jar resolve --manifest mclhr.json --store ./libraries

# JavaFX 扩展（可选）
java -jar mclhr.jar javafx --store ./libraries --hmcl-home %APPDATA%\.hmcl
```

`--store` 可重复，顺序即优先级；`--allow-missing` 跳过缺失库。

## 清单格式

```json
{
  "modules": [
    {
      "id": "game",
      "libraries": [
        {"group": "com.example", "artifact": "engine", "version": "1.0"},
        {"group": "com.example", "artifact": "extra", "version": "1.0", "optional": true},
        {"group": "com.example", "artifact": "native", "version": "1.0",
         "classifier": "win", "sha1": "..."}
      ]
    }
  ]
}
```

兼容顶层 `"libraries": [...]`（视为 `default` 模块）。

## 扁平文件名

```
group_artifact_version[-classifier].jar
```

例如 `org.openjfx:javafx-base:17.0.13:win` →
`org_openjfx_javafx-base_17.0.13-win.jar`。

## 扩展

`com.hibernet.mclhr.ext.javafx.JavaFxSupport` 提供 JavaFX 解析（共享库仓优先，
旧 HMCL 缓存回退），并给出三个标准模块的坐标。核心不依赖 JavaFX。

## 构建

```powershell
.\build.ps1 -RunTests     # 需要 JDK 17+
```

或 Gradle：

```bash
./gradlew build
```

产物：`build/mclhr.jar`。

## 使用方

- FLFHMCL：Agent 通过 `JavaFxSupport` 解析 JavaFX（替代原先的 HMCL 缓存硬编码扫描）；
- FLFHMCLauncher：启动器把共享库/JavaFX 放入 `.flfhmcl/libraries`，后续接入 mclhr 解析。

这些只是使用示例，mclhr 本身独立于它们。
