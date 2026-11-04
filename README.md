# mclhr — multiver class loader handle runtime

> FLFHMCLauncher / FLFHMCL 的共享库与模块化类加载运行时。
> 名称：**m**ulti**v**er **c**lass **l**oader **h**andle **r**untime（mclhr）。
> 协议：GPL-3.0（见 `LICENSE`）。

## 这是什么

mclhr 把“库放在哪里”和“怎么加载”从启动器/Agent 里拆出来，做成独立运行时：

- **共享库仓**：一个或多个目录里放扁平命名的 jar（`group_artifact_version[_classifier].jar`），
  多个实例/版本共用同一份文件；
- **模块清单**：`mclhr.json` 声明依赖的库坐标、校验和；
- **解析与校验**：按坐标在多个 store 中查找、校验 SHA-1、产出 classpath；
- **JavaFX 模块**：从共享库仓（或旧版 HMCL 缓存）解析 JavaFX jar，替代 Agent 里的硬编码扫描。

这是 Lunar Client `ichor` 的等价开源实现（不依赖、不复制其闭源代码），
但更小、更通用，可被任意 Java 启动器复用。

## 目录布局

```
libraries/
  org_openjfx_javafx-base_17.0.13-win.jar
  org_openjfx_javafx-graphics_17.0.13-win.jar
  org_openjfx_javafx-controls_17.0.13-win.jar
  net_fabricmc_fabric-loader_0.16.10.jar
```

## 清单格式（mclhr.json）

```json
{
  "libraries": [
    {
      "group": "org.openjfx",
      "artifact": "javafx-base",
      "version": "17.0.13",
      "classifier": "win",
      "sha1": "optional"
    }
  ]
}
```

## CLI

```bash
java -jar mclhr.jar javafx --store <libraries> [--hmcl-home <%APPDATA%/.hmcl>]
java -jar mclhr.jar resolve --manifest mclhr.json --store <libraries> [--store ...]
```

输出每行一个 jar 的绝对路径，便于启动器/脚本消费。

## 集成

Agent / 启动器只需：

```java
List<Path> jars = MclhrRuntime.resolveJavaFx(hmclHome, store);
// 然后把 jars 追加到系统 classpath（Instrumentation.appendToSystemClassLoaderSearch）
```

mclhr 不依赖 `java.instrument`，只做**解析**；加载策略由宿主决定。

## 构建

```powershell
.\build.ps1
```

产物：`build\mclhr.jar`（要求 JDK 17+）。
