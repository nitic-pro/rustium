# Technical & Mathematical Architecture

本MODはコンピュータサイエンスの力技と数学的近似を用いて、Minecraftの根幹処理を極限まで最適化しています。

## 1. 爆発レイキャストの離散サンプリング化 (Discrete Sampling fallback)

バニラの爆発演算はエンティティのAABB（軸平行境界箱）を細かく分割し、全グリッドの交点に対してレイ（線分）を飛ばす体積積分的なアプローチを取っています。この計算量は対象の体積 $V$ に比例するため $\mathcal{O}(N \times V)$ となり、巨大なスライムや密集した空間での計算爆発を引き起こします。

本MODではこれを空間の代表点による**離散サンプリング**に置き換えます。AABBの中心点 $C$ と8つの頂点集合 $\{V_k \mid k=1\dots8\}$ を定義し、以下の式によって露出率 $E$ を算出します。

$$ E = \frac{1}{9} \left( \delta(C) + \sum_{k=1}^{8} \delta(V_k) \right) \quad \text{where} \quad \delta(P) = \begin{cases} 1 & \text{(if ray is unblocked)} \\ 0 & \text{(if ray is blocked)} \end{cases} $$

これにより空間の計算量が体積に依存しない定数時間 $\mathcal{O}(1)$ へと縮退し、TNTの大量起爆時におけるサーバーのフリーズを回避します。

## 2. 平方根を排除した距離計算の近似 (Non-Euclidean Norm Approximation)

Vanillaの経路探索（Pathfinding）やAI処理では、毎Tick数千回におよぶユークリッド距離（$L_2$ ノルム）の計算が行われます。

$$ d_{Euc}(p_1, p_2) = \sqrt{\Delta x^2 + \Delta y^2 + \Delta z^2} $$

コンピュータのハードウェアにおいて浮動小数点の平方根（`fsqrt`）は極めて重い命令（数十クロックサイクル）を消費します。本MODはこれを、チェビシェフ距離（$L_\infty$ ノルム）とマンハッタン距離（$L_1$ ノルム）の線形結合を用いた**八角形近似（Octagonal Approximation）**へ置き換えます。

$$ d_{approx} \approx \max(|\Delta x|, |\Delta y|, |\Delta z|) + 0.33 \times \min(|\Delta x|, |\Delta y|, |\Delta z|) $$

この乗算すらもビットシフト演算に置換することで、実質的に加算と論理演算のみでバニラに極めて近い円形の距離判定を実現し、CPU負荷を激減させています。

## 3. Rust (FFM API) × SIMD並列化計算

距離計算や複雑なベクトル演算をJavaのループで回すと、オブジェクト指向特有のメモリ断片化（AoS: Array of Structures）によりCPUのキャッシュミスが多発します。
そこで本MODは、エンティティの座標データをプリミティブな連続メモリ（SoA: Structure of Arrays）として確保し、Java 21の **FFM API (Foreign Function & Memory API)** をバイパスしてRust製のネイティブライブラリへ直接オフロードします。

Rust側ではハードウェアの **SIMD (Single Instruction, Multiple Data)** レジスタ（AVX2 / AVX-512など）を直接叩き、$N$ 個のエンティティの座標計算を単一のCPU命令で同時実行します。

$$ \vec{D}_{0 \dots 7} = \sqrt{ \vec{X}_{0 \dots 7}^2 \oplus \vec{Y}_{0 \dots 7}^2 \oplus \vec{Z}_{0 \dots 7}^2 } $$

通常のSISDアーキテクチャでは8サイクルかかる処理を、スカラー演算1発分のクロックでねじ伏せるロマンあふれるアプローチです。