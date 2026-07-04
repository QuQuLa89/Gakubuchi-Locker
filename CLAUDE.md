# Gakubuchi-Locker

PaperMC 1.21.1+ 向けの額縁ロックプラグイン（Kotlin / Gradle）。額縁を個人所有としてロックし、他プレイヤーの破壊・操作を防ぐ。

## コマンド

- ビルド: `.\gradlew.bat build`（shadowJar 込み。成果物は `build/libs/`）
- テスト: `.\gradlew.bat test`

## 構成

- Kotlin 1.9 / Java 21 toolchain / Paper API 1.21.1
- `src/main/resources/plugin.yml` の version は `build.gradle.kts` の `project.version` から展開される（バージョン更新は build.gradle.kts 側のみ変更）
- ロック情報の永続化は SQLite（sqlite-jdbc）
