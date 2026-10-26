# Memento In Abyss: Twilight

为 Memento In Abyss 第二层适配暮色森林的群系、植被、结构、音乐与地图。

运行需要 Minecraft 26.1.2、NeoForge 26.1.2.122 或以上、Memento In Abyss 0.2.0.alpha.6 或以上，以及 Twilight Forest 4.9.3838 或以上。本体和暮色各自的必需依赖也需要安装。附属模组不打包这些依赖。

开发期间，先在本体工程执行 `gradlew.bat publishToMavenLocal`，再在本工程使用：

```text
gradlew.bat -PuseLocalMia=true runClient
gradlew.bat -PuseLocalMia=true runData
gradlew.bat -PuseLocalMia=true build
```

在 IDEA 的 Gradle 任务参数中添加 `-PuseLocalMia=true`，或在个人 Gradle 用户配置中设置 `useLocalMia=true`。不启用此参数时从 `https://maven.ringlo.im/releases` 解析本体，适用于已发布版本和 CI；不要将个人路径或凭证写入项目配置。

群系使用 `mementoinabyss_twilight:*` 命名空间，暮色原结构、Boss 和战利品 ID 保持不变。拆分前的开发存档不提供旧联动 ID 迁移，请使用新世界验证。

群系配对、原版群系复制名单和地标集合统一从 `TwilightBiomeDefinitions` 派生。新增配对后运行 `runData` 即可同步相关数据。深度分段和洞口边界由本体统一处理，本附属仅安装 `Layer2BiomeResolver`，不再注册独立群系源。

本体的原生群系选择配置集中在 `MiaAbyssBiomeDefinitions`。本次统一群系源更换了配置格式，开发存档不提供旧格式迁移。
