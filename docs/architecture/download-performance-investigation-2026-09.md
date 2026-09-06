# 下载性能调查（2026-09-06）

## 范围与证据边界

- 未使用任何 skill；本轮已修改下载实现并完成 JVM 回归测试；仍未进行真机下载测速。
- Kototoro：本地 HEAD `86aa44295`，按当前工作区代码分析，工作区存在其他未提交改动。
- Mihon：本地 HEAD `a81b7bce9`；Aniyomi：本地 HEAD `2f5cf775c`。
- `../Kotatsu-Redo` 不存在，改查官方仓库 `devel` 分支在线代码，在线分支可能变化。
- 下文区分代码确定行为与性能推断；没有用户的源、版本、设备、网络和日志，不能量化各原因占投诉的比例。

## 主要发现

### 1. 阅读器与下载器没有使用同一图片获取链路（高优先级）

`reader/domain/PageLoader.kt:527` 优先调用 `repo.fetchPageResponse()`，否则使用
`repo.getImageClient() ?: okHttp`。`download/ui/worker/DownloadWorker.kt:1438` 只创建源请求，
随后使用注入的全局 `okHttp`，没有调用上述两个接口。

`mihon/MihonMangaRepository.kt:318,346` 分别提供扩展 client 和 `httpSource.getImage()`。
因此下载虽然保留 imageRequest 构造出来的请求头，却可能丢失扩展 client 的拦截器、限流、
token 更新以及扩展图片获取行为。受影响的扩展可能出现“阅读正常、下载重试或失败”。
这是确定的链路差异；实际影响哪些扩展仍需对照日志验证。

“与阅读器一致”开关只改变页并发与请求等待，不会修复该差异。

### 2. 大视频使用 300 秒整请求超时，失败后删除已下载内容（高优先级）

`core/network/NetworkModule.kt:94` 设置连接 20 秒、读取 60 秒、整请求 300 秒。
`DownloadWorker.kt:1927` 的直链视频使用该 client，没有单独覆盖超时；请求没有断点 Range，
`1861` 的异常处理删除输出文件。打开自动重试后，外层可重新调度整个 worker。

OkHttp 的 callTimeout 包含读取响应体，不是“300 秒没数据才超时”。例如 1 GiB 视频以
2 MiB/s 稳定传输也需 512 秒，会触及此上限。该数字为算术示例，不是真机测速。
HLS 是每个分片单独请求，不能把 300 秒错误理解为整集 HLS 的总上限。

依据：[OkHttp 官方源码与 API 注释](https://github.com/square/okhttp/blob/master/okhttp/src/commonJvmAndroid/kotlin/okhttp3/OkHttpClient.kt)。

### 3. HLS 串行取片，失败恢复粒度过大（高优先级）

`DownloadWorker.kt:2030` 逐个请求分片，整片 `body.bytes()`，解密、写入、进度更新后才请求下一片。
没有分片并行、持久化完成清单或单片重试；异常上升到删除整集文件的路径。
视频不使用漫画的 downloadThreads 设置。

高 RTT/小分片时，每片等待累积；中途失败还会放大重传成本。仅以 600 片、每片额外等待
150 ms 计算，串行等待可达 90 秒，不含传输、解密与写盘，不能当作实际可提速幅度。

Android 官方说明多线程 executor 可以加速可拆分下载，同时区分整项下载并行数。
参考 [Media3 下载指南](https://developer.android.com/media/media3/exoplayer/downloading-media) 与
[DemoDownloadService 范例](https://github.com/androidx/media/blob/release/demos/main/src/main/java/androidx/media3/demo/main/DemoDownloadService.java)。
迁移还需考虑现有本地文件索引、离线缓存和导出格式，不建议直接替换整个系统。

### 4. 默认人为等待、按作品并发与不准确的限流叠加

`core/prefs/AppSettings.kt:1996`：对齐阅读器默认 false，页并发默认跟随 readerThreads（默认 3），
同时作品数默认 5，请求等待默认 1600 ms，重试次数默认 5、间隔默认 2000 ms。
请求等待只对 `repo.isSlowdownEnabled()` 为真的源生效；内置解析器还会读取源配置。

`DownloadSlowdownDispatcher.kt:35` 在等待之前记录当前时间，没有预订下一次可执行时间。
多个同时到达的请求可能等待到近似相同时间再同时发出；它不是严格的源级间隔调度器。
因此既增加空等，也不能可靠平滑突发。阅读器预取也使用该 dispatcher，前台正常取页不走该等待。

`ActiveDownloadRegistry.kt` 按 worker 注册顺序限制作品数量，没有按源分组。
同源多本书可能合计制造多倍页并发，更容易遇到源站限流；其他源则可能在队列外等待。
不能将 1600 ms 简单折算成全应用固定图片/秒速率。

### 5. 重试和压缩包写入占用页并发名额

`DownloadWorker.kt:892` 的 permit 覆盖解析 URL、网络、重试等待、文件复制和 `output.addPage()`。
`958` 默认最多执行初次请求加 5 次重试。三个慢请求可占满默认三个名额，健康页面无法开始。
章节也是串行的，尾部慢页会拖住后续章节。

`local/data/output/LocalMangaDirOutput.kt:62` 和 `LocalMangaZipOutput.kt:68` 使用互斥写入。
`core/zip/ZipOutput.kt:22` 默认 Deflater 压缩；常见 JPEG/WebP/PNG 已压缩，再压缩可能收益很低。
下载的典型路径是网络临时文件 → 章节暂存副本 → 临时 ZIP → 最终存储。
其中明确存在额外复制；是否成为主要瓶颈取决于 CPU、图片和存储，需用 Perfetto/分段计时确认。

### 6. 漫画续下先联网，再跳过完成章节（确定的额外请求）

`DownloadWorker.kt:773` 先 `getPages()`，到 `788` 才判断 chaptersToSkip。
对包含大量完成章节的任务，仍会逐章请求页面列表，甚至在跳过前进入重试。
EPUB 特判是当前顺序的一部分，修复应保留其独立完成状态语义。
小说分支 `1053` 和视频分支 `1776` 已先检查完成状态。

### 7. Retry-After 日期形式被误当成时长（确定的协议处理错误）

`core/network/RateLimitInterceptor.kt:28` 对日期形式返回 epoch 毫秒。
`parser-api/.../TooManyRequestExceptions.kt:15` 将传入值当成时长，再加到当前时间。
因此本来几十秒后的重试时间会变成极大等待，超过 `runFailsafe` 两小时阈值并进入暂停路径。
纯秒数形式不受这个错误影响；没有 Retry-After 时则进入现有未知延迟暂停策略。

依据：[RFC 9110 §10.2.3](https://www.rfc-editor.org/rfc/rfc9110.html#name-retry-after)，字段允许绝对日期或相对秒数。

### 8. 小说并发设置与用户理解不一致

`DownloadWorker.kt:1051` 按章节串行；`1118` 按插图串行，失败图片之后再重试。
漫画页并发设置不作用于这条路径；`1232` 还支持额外章节延迟（默认 0）。
纯文本吞吐受章节解析/站点时延限制，插图丰富的小说还会累积图片等待和写包成本。

### 9. 后台排队与展示需要独立诊断

Android 16 起，长时间前台 WorkManager 任务也可能耗尽 JobScheduler 配额。
Kototoro 每本作品一个 expedited worker，先启动再由应用层等待并发名额。
这值得检查，但没有调度日志，不能断言用户慢速由系统配额造成。
依据：[Android 长时间 worker 文档](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running)。

漫画进度在页面写包成功后上报，按页而非字节计数；视频每 256 KiB 调用进度路径，通知与
WorkManager progress 有 400 ms 节流，但通知对象先构建。当前展示不能可靠区分联网、重试、
限流、压缩和最终写入，用户看到的不动不一定是网络吞吐低。

## 与其他项目的比较

| 项目 | 已检查实现 | 能借鉴的点与局限 |
| --- | --- | --- |
| Mihon | `Downloader.kt:197,368,478`；`DownloadPreferences.kt` | 默认最多 5 个源、每源一个章节、章内默认 5 页；使用 source.getImage；206 时追加，416 清理；全章下载后可选归档。其重试仍占页流水线并发，同样受慢源/存储约束。 |
| Aniyomi | `AnimeDownloader.kt:364,469,500` | 内部采用 FFmpeg，也支持外部下载器；明确在 IO context 处理。不能据此声称默认多连接或必然更快。 |
| Kotatsu-Redo | 在线 `DownloadWorker.kt` | 章内并发常量 4；先跳过完成章节再 getPages；仍将 runFailsafe 与 addPage 放在 permit 内，存在相似结构限制。 |

Kotatsu-Redo 代码：[官方 DownloadWorker](https://github.com/Kotatsu-Redo/Kotatsu-Redo/blob/devel/app/src/main/kotlin/org/koitharu/kotatsu/download/ui/worker/DownloadWorker.kt)。

## Issues 交叉验证

- [Mihon #1307](https://github.com/mihonapp/mihon/issues/1307)：用户报告在线阅读正常、下载仅 40–60 KB/s，版本 0.16.5；这是症状记录，不是根因或当前版本性能证据。
- [Kototoro #128](https://github.com/Kototoro-app/Kototoro/issues/128)：用户提到下载位置、速度和删除耗时；不能由其主观描述推出 Android/data 本身必然慢。
- [Aniyomi #157](https://github.com/aniyomiorg/aniyomi/issues/157)：高速下载伴随低端机卡顿和发热的报告，提示优化也需看 CPU/存储与交互成本。
- [Kotatsu-Redo #47](https://github.com/Kotatsu-Redo/Kotatsu-Redo/issues/47)：同源大量更新遇到 429；发生在更新检查，支持关注源级限流，但不是下载慢的直接证明。

本次公开检索不足以重建“很多用户”的完整样本，未把 issue 数量当作故障发生率。

## 已实现与后续验收

1. 已修复：下载复用扩展图片获取接口；视频专用超时策略；Retry-After 日期转换；已完成普通章节提前跳过。
2. 已实现恢复：直链 `.part` + 条件 Range/206 校验；HLS 持久化分片完成记录；只在临时文件完整后发布最终文件。
3. 已实现结构优化：源级冷却改为预约式调度，网络 permit 与 SAF/ZIP 写入解耦；HLS 三路有界并行且按顺序组装。
4. 已减少漫画页的一次完整复制：下载临时文件与章节暂存位于同一普通文件目录时直接重命名，SAF/缓存文件仍安全回退为复制。
5. 已增加图片传输日志，记录源、限流等待、传输耗时、字节数和有效字节速率，便于定位慢在网络还是写入。

仍需真机/网络矩阵验收的工作：

6. 用同一批真实漫画图片比较 ZIP 默认压缩与低/无压缩；继续评估 SAF 场景下的暂存复制成本。
7. 将当前单次图片传输日志汇总成按源聚合的 p50/p95、有效字节/秒、请求数、429、重复下载量与 CPU，覆盖任务排队、URL 解析、DNS/TLS/首字节、重试、写包和最终复制阶段。

应执行但本次未执行的回归实验：

- MockWebServer 持续输出超出缩短后的 callTimeout：验证视频不会被错误整请求超时终止。
- 日期与秒数 Retry-After、过去日期、异常字段，验证等待/降级语义。
- 100 个已完成章节加一个新章：已完成普通章节页面请求数应为 0。
- 扩展自定义 getImage/client：阅读与下载均走相同源语义。
- 一个坏页加若干正常页：观测名额占用及错误恢复，避免整章被无谓拖住。
- HLS 可控 RTT、断连与恢复：验证只补缺失片段，输出顺序和解密正确。
- 相同源/章节/清晰度/网络、冷热缓存分组、内部存储与 SAF 分组，比较 wall time 与网络实际吞吐。

本轮已运行 `:app:compileDebugKotlin` 和 `RateLimitInterceptorTest`；两者均通过。真机吞吐、源站差异、SAF 写入和压缩收益仍需在固定网络矩阵中测量，文中的算术示例不是测速结果。
