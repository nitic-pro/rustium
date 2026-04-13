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
   * バニラのTNT爆発における重い「線分レイキャスト計算」を極限まで間引きし、エンティティの「中心と当たり判定の8つの頂点」の合計9点のみで判定を行います。
   * バニラに近いダメージ減衰の挙動を保ちつつ、大量のTNTが爆発した際の計算量を劇的に減らし、サーバーフリーズを防ぎます。

## � 技術的・数学的背景 (Architecture & Mathematics)
本MODはコンピュータサイエンスの力技と数学的近似を用いて、Minecraftの根幹処理を極限まで最適化しています。

### 1. 爆発レイキャストの離散サンプリング化 (Discrete Sampling fallback)
バニラの爆発演算はエンティティのAABB（軸平行境界箱）を細かく分割し、全グリッドの交点に対してレイ（線分）を飛ばす体積積分的なアプローチを取っています。この計算量は対象の体積 $V$ に比例するため $\mathcal{O}(N \times V)$ となり、巨大なスライムや密集した空間での計算爆発を引き起こします。

本MODではこれを空間の代表点による**離散サンプリング**に置き換えます。AABBの中心点 $C$ と8つの頂点集合 $\{V_k \mid k=1\dots8\}$ を定義し、以下の式によって露出率 $E$ を算出します。

$$ E = \frac{1}{9} \left( \delta(C) + \sum_{k=1}^{8} \delta(V_k) \right) \quad \text{where} \quad \delta(P) = \begin{cases} 1 & \text{(if ray is unblocked)} \\ 0 & \text{(if ray is blocked)} \end{cases} $$

これにより空間の計算量が体積に依存しない定数時間 $\mathcal{O}(1)$ へと縮退し、TNTの大量起爆時におけるサーバーのフリーズを回避します。

### 2. 平方根を排除した距離計算の近似 (Non-Euclidean Norm Approximation)
Vanillaの経路探索（Pathfinding）やAI処理では、毎Tick数千回におよぶユークリッド距離（$L_2$ ノルム）の計算が行われます。

$$ d_{Euc}(p_1, p_2) = \sqrt{\Delta x^2 + \Delta y^2 + \Delta z^2} $$

コンピュータのハードウェアにおいて浮動小数点の平方根（`fsqrt`）は極めて重い命令（数十クロックサイクル）を消費します。本MODはこれを、チェビシェフ距離（$L_\infty$ ノルム）とマンハッタン距離（$L_1$ ノルム）の線形結合を用いた**八角形近似（Octagonal Approximation）**へ置き換えます。

$$ d_{approx} \approx \max(|\Delta x|, |\Delta y|, |\Delta z|) + 0.33 \times \min(|\Delta x|, |\Delta y|, |\Delta z|) $$

この乗算すらもビットシフト演算に置換することで、実質的に加算と論理演算のみでバニラに極めて近い円形の距離判定を実現し、CPU負荷を激減させています。

### 3. Rust (FFM API) × SIMD並列化計算
距離計算や複雑なベクトル演算をJavaのループで回すと、オブジェクト指向特有のメモリ断片化（AoS: Array of Structures）によりCPUのキャッシュミスが多発します。
そこで本MODは、エンティティの座標データをプリミティブな連続メモリ（SoA: Structure of Arrays）として確保し、Java 21の **FFM API (Foreign Function & Memory API)** をバイパスしてRust製のネイティブライブラリへ直接オフロードします。

Rust側ではハードウェアの **SIMD (Single Instruction, Multiple Data)** レジスタ（AVX2 / AVX-512など）を直接叩き、$N$ 個のエンティティの座標計算を単一のCPU命令で同時実行します。

$$ \vec{D}_{0 \dots 7} = \sqrt{ \vec{X}_{0 \dots 7}^2 \oplus \vec{Y}_{0 \dots 7}^2 \oplus \vec{Z}_{0 \dots 7}^2 } $$

通常のSISDアーキテクチャでは8サイクルかかる処理を、スカラー演算1発分のクロックでねじ伏せるロマンあふれるアプローチです。

## �🤝 互換性 (Compatibility)

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
git clone https://github.com/nitic-pro/rustium.git
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
