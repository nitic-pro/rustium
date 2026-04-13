# Rustium 🚀

Minecraft 26.1.2 向けの極限最適化（ロマン追求型）Fabric MODです。
通常では到達できない領域のパフォーマンスを引き出すため、Rustによる強力な**ネイティブ処理（SIMD）のオフロード**と、Vanillaの仕様の隙間を突いた**強引かつ超軽量な近似アルゴリズム**を導入します。

## 🌟 特徴 (Features)

1. **Rustネイティブ演算へのオフロード (FFM API)**
   * マイクラ内で発生する膨大な「距離計算（平方根）」を、Javaのレイヤーから切り離し、Rust言語で書かれたネイティブライブラリへ直接パスします。
   * Rust側のSIMD（Single Instruction, Multiple Data）命令を活用し、複数エンティティ同時の距離計算を一瞬で終わらせます。
   * Windows (`.dll`), macOS (`.dylib`), Linux (`.so`) 全対応のクロスプラットフォーム設計。

2. **経路探索（パスファインディング）の整数近似化**
   * モブ（ゾンビなど）の移動時に毎ティック呼ばれる `NodePathfinding` 距離計算において、厳密な浮動小数点計算（`Math.sqrt`）を廃止。
   * マンハッタン距離とチェビシェフ距離を組み合わせた「高速な整数近似アルゴリズム」に置き換えることで、大量のモブが押し寄せてもサーバーのTPSが落ちません。

3. **超軽量爆発レイキャスト (Lithium互換)**
   * バニラのTNT爆発における重い「線分レイキャスト計算」を極限まで間引きし、エンティティの「足元・中心・頭」の3点のみで判定を行います。
   * これにより、大量のTNTが爆発した際のサーバーフリーズを劇的に緩和します。

## 🤝 互換性 (Compatibility)

本MODは、**Lithium 等のメジャーな最適化MODと完全共存可能**です。
* バニラのメソッドを破壊する `@Overwrite` アノテーションを一切使用していません。
* すべて `@Inject(at = @At("HEAD"), cancellable = true)` を用いて実装されており、Lithiumの処理より先に独自の軽量化ロジックを出力し、競合（クラッシュ）を回避する設計になっています。

## 📥 導入方法 (Installation)

### 必要動作環境
* Minecraft 26.1.2
* Fabric Loader
* Fabric API
* **Java 21 以降** (FFM API を有効にするため)

### 起動オプション (JVM引数)
本MODは最新のJavaネイティブ機能を使用しているため、ゲームの起動時またはサーバーの起動スクリプトのJVMパラメータに以下を必ず追加してください。
```text
--enable-native-access=ALL-UNNAMED
```

## 🛠 ビルド方法 (How to Build)

ご自身でソースコードからビルドする場合：
```bash
# リポジトリのクローン
git clone https://github.com/YourName/rustium.git
cd rustium

# Fabricビルドの実行
./gradlew build
```
ビルドが成功すると、`build/libs/` ディレクトリの中に `rustium-1.0.0.jar` が生成されます。これを `mods` フォルダに入れてください。

## 📊 パフォーマンス測定 (Benchmark)

ゲーム内でクリエイティブモード（または権限ありのサバイバル）に入り、以下のコマンドを実行することで、現在の最適化がどれほど機能しているかをリアルタイムで測定できます。

```text
/rustiumbench
```

自動的にテスト用のガラスプラットフォームが生成され、プレイヤーの周囲に大量のゾンビがスポーンし、秒間数千〜数万回の最適化メソッド（SIMD Calls / Pathfinding Callsなど）が呼び出されていることをチャット欄で確認できます。

---
*Created as a Technical Proof of Concept.*
