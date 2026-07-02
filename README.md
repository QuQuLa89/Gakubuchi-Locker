# Gakubuchi-Locker

PaperMC サーバー向けの額縁ロックプラグインです。額縁（ItemFrame / GlowItemFrame）を個人所有としてロックし、他プレイヤーによる破壊・アイテム操作・回転を防止します。

---

## 動作環境

| 項目 | 要件 |
|------|------|
| サーバー | [PaperMC](https://papermc.io/) 1.21.1 以上 |
| Java | 21 以上 |
| 依存プラグイン | なし（スタンドアロン動作） |

---

## 機能

### 額縁の保護

ロックされた額縁に対して以下の操作をすべてブロックします。

- **破壊防止** — 左クリックによる打撃、HangingBreak イベント
- **爆発・ピストンによる破壊防止** — TNT・クリーパー爆発、ピストン押し出し等
- **アイテム出し入れ防止** — 右クリックによるアイテム設置・回収
- **回転防止** — 右クリックによる額縁内アイテムの回転

### オーナー制

- ロックした本人のみアンロック可能
- 他プレイヤーはロック済み額縁を一切操作できない
- **オーナー本人はロック中でもアイテムの出し入れ・破壊が可能**（破壊時はロック情報も自動削除）
- **OP はオーナーでなくても額縁を破壊可能**（破壊時はロック情報も自動削除）

### 設置時の自動ロック（v1.1.0〜）

- 額縁を設置すると**自動でロックされ**、設置者がオーナーになります
- `/gakubuchilock` コマンドを使わずに即座に保護されます

### データ永続化

- ロック情報は **SQLite** に保存し、サーバー再起動後も引き継がれます
- 起動時に全データをメモリキャッシュへ展開するため、保護判定は **O(1)** で処理されます

---

## インストール

1. [Releases](../../releases) から最新の `Gakubuchi-Locker-<version>.jar` をダウンロード
2. サーバーの `plugins/` フォルダへ配置
3. サーバーを起動（または `/reload` を実行）

初回起動時に `plugins/Gakubuchi-Locker/gakubuchi.db` が自動生成されます。

---

## コマンド

| コマンド | 説明 |
|----------|------|
| `/gakubuchilock on` | ロックモードを有効にする。以降、額縁を左クリックするとロックされる |
| `/gakubuchilock off` | ロックモードを終了する |
| `/gakubuchiunlock on` | アンロックモードを有効にする。以降、自分がロックした額縁を左クリックで解除できる |
| `/gakubuchiunlock off` | アンロックモードを終了する |

コマンドにはタブ補完（`on` / `off`）が実装されています。

---

## パーミッション

| パーミッション | デフォルト | 説明 |
|----------------|-----------|------|
| `gakubuchilocker.use` | `true`（全プレイヤー） | コマンドの使用を許可する |

`default: true` のため、全プレイヤーが初期状態でコマンドを使用できます。  
特定グループに制限したい場合は LuckPerms 等の権限プラグインで `gakubuchilocker.use` を `false` に設定してください。

---

## 使い方（フロー）

```
# 額縁をロックする手順
1. /gakubuchilock on  → ロックモード ON
2. ロックしたい額縁を左クリック
3. 「額縁をロックしました！」と表示されればOK
4. /gakubuchilock off → モード終了（そのまま放置でも可）

# 額縁のロックを解除する手順
1. /gakubuchiunlock on  → アンロックモード ON
2. 自分がロックした額縁を左クリック
3. 「額縁のロックを解除しました！」と表示されればOK
4. /gakubuchiunlock off → モード終了
```

---

## 技術仕様

| 項目 | 詳細 |
|------|------|
| 言語 | Kotlin 1.9.22 |
| ビルドツール | Gradle（Kotlin DSL） |
| パッケージング | Shadow JAR（sqlite-jdbc を同梱） |
| データベース | SQLite（`org.xerial:sqlite-jdbc:3.45.1.0`） |
| API | Paper API 1.21.1-R0.1-SNAPSHOT |

### データベーススキーマ

```sql
CREATE TABLE IF NOT EXISTS locked_frames (
    entity_uuid TEXT      PRIMARY KEY,
    world       TEXT      NOT NULL,
    x           INTEGER   NOT NULL,
    y           INTEGER   NOT NULL,
    z           INTEGER   NOT NULL,
    owner_uuid  TEXT      NOT NULL,
    locked_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## ビルド

```bash
./gradlew build
```

生成物: `build/libs/Gakubuchi-Locker-<version>.jar`

---

## 変更履歴

### v1.1.1

- **OP による破壊を許可**: OP 権限を持つプレイヤーは、設置者（オーナー）でなくてもロック済みの額縁を破壊できるようになりました。破壊時はロック情報も自動削除されます

### v1.1.0

- **自動ロック**: 額縁を設置すると自動でロックされ、設置者がオーナーになります。`/gakubuchilock` コマンドは不要になりました（引き続き手動ロックにも使用できます）
- **オーナー権限の拡張**: ロック中の額縁でもオーナー本人はアイテムの出し入れ・破壊が可能になりました。破壊時はロック情報も自動削除されます

### v1.0.0

- 初回リリース

---

## ライセンス

MIT License
