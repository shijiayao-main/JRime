# Path B 前置精读:librime 音节图生产侧语义规格(syllabifier)

> 2026-11-20 · 期二 T0 续单(researcher)产出 · Path B PoC 第一前置(设计文档 §2.6 硬门槛)。
> 目标:Kotlin 实现者拿这份规格,不读 C++ 源码即可复刻 SyllableGraph 构造行为。
> 结论一句话:**运行时音节匹配是纯 prism 驱动的**——编译期 algebra 经 Projection 物化进 prism.bin(Darts 双数组 + SpellingMap),运行时 Syllabifier 对 raw input 做按位置优先队列遍历 + prism 前缀展开 + 三阶段后处理;Kotlin 复刻不需要移植 algebra 运行时(除非 schema 用 translator/canonicalize)。

## Q1 SyllableGraph 数据结构全貌

图 = 「位置为顶点、spelling 为边」的有向无环多重图;消费侧实际读取的是反排视图 indices。

定义(syllabifier.h):

- **SyllableGraph**(syllabifier.h:40-46):
  - `input_length`:原始输入长度(syllabifier.cc:267)。
  - `interpreted_length`:farthest,图实际消耗到的最远位置(syllabifier.cc:268);函数返回值即它(:274)。
  - `vertices: VertexMap = map<size_t, SpellingType>`(:32):位置 → 该位置被到达路径的最优(数值最小)SpellingType;同位置更差类型直接丢弃(syllabifier.cc:47-53)。
  - `edges: EdgeMap = map<size_t, EndVertexMap>`,`EndVertexMap = map<size_t, SpellingMap>`,`SpellingMap = map<SyllableId, EdgeProperties>`(:31-34):edges[起点][终点][音节id] = EdgeProperties。无显式 node 列表;多路径 = 同一对(start,end)并列多个 syllable_id,或同一 syllable_id 出现在不同 end。
  - `indices: SpellingIndices = map<size_t, SpellingIndex>`,`SpellingIndex = map<SyllableId, SpellingPropertiesList>`(:36-38):由 Transpose 反排生成,indices[start][syll_id] = list<const EdgeProperties*>,按 end 降序 append(syllabifier.cc:311-321)。**消费侧实际读取的视图**(table.cc:585-596、user_dictionary.cc:257)。

- **EdgeProperties**(syllabifier.h:24-29)继承 SpellingProperties(spelling.h:24-35):
  - `type: SpellingType`(spelling.h:15-22,枚举序即优先级:kNormalSpelling(0) < kFuzzySpelling(1) < kAbbreviation(2) < kCompletion(3) < kAmbiguousSpelling(4) < kInvalidSpelling(5))。
  - `end_pos`:边终点(建图写回,syllabifier.cc:134、248)。
  - `credibility: double`:累计惩罚权重,负值更好为 0(Q3)。
  - `tips: string`:拼写提示(编译期 algebra 产生)。
  - `is_correction: bool`:纠错标记(syllabifier.cc:137-141 或编译期 calculus.cc:249-256)。
  - `ambiguous_source_positions: set<size_t>`:切分歧义编码段起始位置,CheckOverlappedSpellings 写入(:300-303)。
- **SyllableId = int32_t**(:22):table.bin 音节表下标,非 spelling 下标;spelling_id(双数组 value)经 SpellingMap 翻译成 syllable_id。

## Q2 构造主链:raw input → SyllableGraph

按键层 Speller(gear/speller.cc:100-146)只做字母入栈与自动上屏,**不做切分**;建图链 = ScriptTranslator::Query(gear/script_translator.cc:214-242)→ ScriptTranslation::Evaluate(:460-461)→ ScriptSyllabifier::BuildSyllableGraph → Syllabifier::BuildSyllableGraph(algo/syllabifier.cc:31-275)。算法 = 以位置排序的优先队列 + 逐顶点 prism 前缀展开的单向 BFS(无回溯)。

伪代码(逐行对应 syllabifier.cc):

```
queue = 最小堆 Vertex{pos, type},按 (pos, type) 字典序升序弹出   # :20-22
push {0, kNormalSpelling}; farthest = 0
while queue 非空:
    v = pop; if vertices[v.pos] 已存在: continue      # 首访即最优类型,其余丢弃 :47-53
    vertices[v.pos] = v.type; farthest = max(farthest, v.pos) :55-56
    begin = v.pos; 跳过 leading delimiters              # :59-62(delimiter 透明字符)
    # ---- 查询本顶点能走出的所有 spelling ----
    if canonicalizer_ 存在:                             # :71-85(运行时 algebra,罕见)
        for len in 1..min(剩余长度, prism.max_key_length):
            s = input[begin..begin+len); canonicalizer_->Apply(&s)  # 逐长度重写后精确查
            if prism.GetValue(s) -> matches += {value, len}
    else:
        prism.CommonPrefixSearch(input[begin..], &matches)           # :87 主路径
    if corrector_ 存在:                                  # :89-106(纠错,见 Q4)
        ToleranceSearch(...) 追加 {syllable_id, len} 匹配
    # ---- 展开成边 ----
    for m in matches (m.length==0 跳过 :112-113):
        end = v.pos + leading_gap + m.length; 再吃掉 trailing delimiters :114-118
        for desc in prism.QuerySpelling(m.value):        # spelling → 音节集合 :126-155
            if strict_spelling && 整段输入被此边覆盖 && desc.type != kNormal: 丢弃 :130-133
            props = desc.properties; props.end_pos = end
            if 纠错来源: props.is_correction=true; props.credibility = -4.6052 :137-141
            spellings[end][syll_id] 已存在则取 min(type),否则插入 :142-147
        本边最优 type = min(desc.type) 且不含 correction :148-152
        路径 type = max(本边最优, v.type)                 # 路径质量=最差段 :161-166
        push {end, path_type}
# ---- 后处理 ----
stale 裁剪: good={farthest}; i 从 farthest-1 降序:
    删不连回 good 的边;删 type > last_type(=max(vertices[farthest], kFuzzy))的非纠错 spelling :174-213
    顶点 type 过差或无出边则删顶点 :214-219
    非缩写边调用 CheckOverlappedSpellings(歧义关节标记) :209-211
if enable_completion && farthest < input.length:         # :224-265
    prism.ExpandSearch(input[farthest..], limit=512)     # 补全
    对 m.length >= 剩余长度的匹配: type<kAbbreviation 的 spelling
    → type=kCompletion, credibility += -2.9957, end_pos=input.length
    加边 [farthest, input.length); farthest = input.length
input_length=input.length; interpreted_length=farthest :267-268
Transpose(graph)  # 反排 indices :272
```

prism 查询原语选择:主路径永远 CommonPrefixSearch(prism.cc:279-286);仅当 schema 配 translator/canonicalize 时改用「逐长度截断→Projection::Apply→GetValue(exactMatchSearch,prism.cc:268-275)」(重写打破前缀单调性,syllabifier.cc:80-82);ExpandSearch(:288-329,先 traverse 前缀再按 alphabet BFS 扩展)只用于 completion 与 LookupWords 预测。**音节边界判定 = "trie 里存在这个前缀的 key"——没有音节库参与,边界集合完全由 prism 键集决定。**

max_key_length(prism.h:49):运行时仅用于 canonicalizer 路径截断上限(:72-73),普通 CommonPrefixSearch 路径不需要(trie 深度自然封顶)。

## Q3 credibility 语义

credibility = 贯穿「编译期规则 → prism 文件 → 图边 → chunk → 候选 weight」的加法惩罚链,全部 ≤0 的 log 概率值;初值编译期写死进 SpellingMap,运行时只在两处追加(纠错、completion)。

| 来源 | 值 | 写入点 |
|---|---|---|
| 正常全拼 | 0.0 | SpellingProperties 默认(spelling.h:27) |
| fuzz | -0.6931(log 0.5) | calculus.cc:15,218 |
| abbrev | -0.6931(log 0.5) | calculus.cc:14,243 |
| derive | 0.0(不改 type) | calculus.cc:158-197 |
| correction(编译期) | -4.6052(log 0.01) | calculus.cc:16,253 |
| correction(运行时 corrector) | -4.6052 | syllabifier.cc:29,137-141 |
| completion | -2.9957(log 0.05) | syllabifier.cc:28,247 |

编译期合并语义(Script::Merge → SpellingProperties::Compose/Update,spelling.cc:5-39):同串多次推导 Compose = type 取更差者 + credibility 累加(:11);同一 (spelling→音节) 重复出现 Update = credibility 取最大者(:34-36)、tips 清空(:38)。

运行时传播:建图把 desc.credibility(文件 float,prism.h:23,29;读出 widen double,prism.cc:69-79)抄到 EdgeProperties(:129)。Table::Query 逐音节累加:next_credibility = props->credibility(+歧义惩罚,当前 if(false) 禁用,table.cc:601-610),TableQuery::Advance/Access 维护前缀和(table.cc:102-115,190-193);Lookup 的 lookup_table 再加 initial_credibility + a.credibility()(dictionary.cc:322);最终 entry_->weight = e.weight - log(1e8) + chunk.credibility(dictionary.cc:212-213)。UserDictionary 同样累加(user_dictionary.cc:278-279,439)。

SpellingDescriptor.credibility 角色 = 编译期惩罚唯一载体(prism.h:25-31,Prism::Build 从 Script 写入 prism.cc:238-252),建图经 SpellingAccessor::properties() 读出(prism.cc:69-80)。**float→double 精度**:文件 float,读出 widen double,Kotlin 必须 Float.toDouble() 同构。

歧义惩罚现状:table.cc:601-609 在 if(false) 死块;生产侧仍标 ambiguous_source_positions(:300-303)但消费侧不生效——Kotlin 复刻保留标记不实现惩罚,但标记本身必须保留(IsCorrection/Syllabify 不读,golden 覆盖此路径需行为一致)。

## Q4 algebra 运行时角色边界

algebra 99% 是编译期概念;运行时只剩两个可选旁路(默认 schema 都不走)。

- 编译期:DictCompiler::BuildPrism 读 speller/algebra(dict_compiler.cc:328)→Projection::Load(algebra.cc:54-86)→table 音节表灌进 Script(AddSyllable,algebra.cc:14-20)→Projection::Apply(algebra.cc:107-140):deletion 规则删原 spelling、addition 规则加新 spelling(erase=只删、derive/fuzz/abbrev=只加不删、xform=删旧加新)。fuzz/abbrev/correction 在 calculus.cc:214-256 打 type+credibility。产物 Script(spelling串→Spelling列表)整体物化进 prism:trie 键=spelling 串(prism.cc:153-165),SpellingMap 每键挂 descriptor 列表(prism.cc:215-256)。无 algebra 时 trie 键=音节本身、每 spelling_id 单 descriptor 且 syllable_id=自身下标(prism.cc:142-168)。
- 运行时旁路 1 canonicalizer:schema 配 translator/canonicalize 时加载(script_translator.cc:200-205),建图时逐候选前缀重写后精确查(syllabifier.cc:71-85)——查询前改写,不参与结果合并。
- 运行时旁路 2 corrector:translator/enable_correction: true 时 Syllabifier::EnableCorrection(:89-106,323-325);CorrectorComponent::Create 无条件返回 NearSearchCorrector(EditDistanceCorrector 被 #if 0,corrector.cc:302-321),ToleranceSearch=邻键替换+trie.traverse 受限编辑距离(:19-60 keyboard_map、247-289);命中打 is_correction + credibility=-4.6052(:137-141),纠错边在 stale 裁剪豁免(:193-197)。
- 结论:fuzz/abbrev/derive 已完全物化进 prism SpellingMap(type 位+credibility+tips),运行时不重放规则。**Path B(运行时只读 bin)不需要实现 algebra/calculus**,只需 canonicalizer 与 corrector 在 schema 配置存在时——本项目常量生成期可直接决定是否需要这两旁路。

## Q5 边界与陷阱

- remaining_code:不在图里!它是 dictionary.cc:21-70 Chunk 的字段,语义=预测匹配未打完的音节后缀:LookupWords 中 prism 匹配长度>输入长度时取 syllable.substr(code_length)(dictionary.cc:404-409),展示为 comment ~xxx(:215-218)。图构造侧只有 interpreted_length。
- 多路径歧义(xian = xi'an / xian):两条路径自然表达(边 [0,4) 与 [0,2)+[2,4),撇号为 delimiter 透明吃掉)。CheckOverlappedSpellings(:277-309)把中间顶点标 kAmbiguousSpelling、给后段边写 ambiguous_source_positions;消费惩罚死代码(table.cc:602)。**注意顶点 type 会被改成 kAmbiguousSpelling**(:303),影响 stale 裁剪的 last_type 比较,必须复刻。
- stale 裁剪是最隐蔽的陷阱(:174-222):不连回 farthest 的边/顶点全删;type 差于 max(farthest 顶点 type, kFuzzySpelling) 的 spelling 被删(如 farthest 全拼时,末段纯 abbrev 顶点整棵砍);纠错边豁免。不复刻→候选集超集/顺序错乱。
- 顶点首访丢弃(:47-53):同位置更差类型不产生第二次展开——依赖优先队列 (pos,type) 升序弹出,Kotlin 用同序比较器。
- input 含非 alphabet 字符:delimiter 前后透明消费(:59-62,116-118)且计入边跨度;大写等在 trie 无 label→CommonPrefixSearch 断路→该顶点无出边→stale 裁剪删除→interpreted_length 截断。上游 speller.cc:109 已过滤非法键,但 translator Query 仍可能收到含杂字符文本,防御="无匹配即死路"。
- 空串:直接返回 0,graph 全空(:34-35)。
- 无匹配路径:farthest=0、vertices 只剩 {0}、interpreted_length=0;Table::Query 因 start_pos >= interpreted_length 直接失败(table.cc:575)→无候选。
- completion 匹配条件:m.length < code_length 跳过(:236-237)——只收"覆盖剩余全部输入"的键;只收 type < kAbbreviation(:245),且只在没有普通路径到达 farthest 时触发。
- ExpandSearch 上限 512(:226)且结果依赖 BFS 遍历序(alphabet 序+队列 FIFO,prism.cc:309-328)——截断集合必须同序复刻。
- CommonPrefixSearch 结果上限=key.length()(:282-284 先 resize 再缩),不会超剩余输入长度。

## Q6 Kotlin 复刻注记

最小数据结构清单:

1. PrismFile(mmap prism.bin):Metadata 320 字节定长头+darts 单元数组+SpellingMap。布局(mapped_file.h OffsetPtr=相对自身 int32 偏移、Array=inline size+元素、List=size+OffsetPtr 推导):
   - Metadata(prism.h:36-50):format[32]("Rime::Prism/5.0",兼魔数,prism.cc:32,102-107)@0;dict_file_checksum@32、schema_file_checksum@36、num_syllables@40、num_spellings@44、double_array_size@48(单元数,每单元 4 字节);double_array OffsetPtr@52(数据地址=52+offset);spelling_map OffsetPtr@56;alphabet[256]@60(ExpandSearch 用,:307-308);max_key_length@316。
   - SpellingMap=Array<SpellingMapItem>:uint32 size+inline items(mapped_file.h:60-68);SpellingMapItem=List<SpellingDescriptor>:uint32 size+OffsetPtr(descriptor 地址=item地址+4+int32@item+4,mapped_file.h:40-44,70-78);SpellingDescriptor:int32 syllable_id、int32 type(bit30=is_correction,prism.cc:27-28)、float credibility、String tips(OffsetPtr<char>,NUL 结尾)=16 字节。
   - darts 单元解码(darts.h:50-80):has_leaf=(u>>8)&1;value=u&0x7fffffff;label=u&0x800000FF;offset=(u>>>10)<<((u>>>9)&1?8:0)。搜索原语照抄 darts.h:405-515 的 exactMatchSearch/commonPrefixSearch/traverse(节点推进 node_pos ^= unit.offset()^key[i],叶子 node_pos^unit.offset() 取 value;-1=未命中,traverse -2=路径断)。
   - 音节字符串不在 prism 里,需同时解析 table.bin 的 syllabary(table.cc:536-549)才能 syllable_id→串(preedit/Spell 用)。
2. SyllableGraph:inputLength/interpretedLength:Int、vertices: SortedMap<Int, Int>、edges: SortedMap<Int, SortedMap<Int, SortedMap<Int, EdgeProps>>>、indices: SortedMap<Int, SortedMap<Int, List<EdgeProps>>>(Transpose 后只读)。
3. EdgeProps(type, endPos, credibility: Double, tips, isCorrection, ambiguousSourcePositions: Set<Int>)。

prism mmap 解析可行性:完全可行且是推荐路径——文件无压缩、无变长编码(除 NUL 串)、全小端 32 位布局,darts 单元=裸 IntArray,Android arm64/x86_64 均小端。要点:OffsetPtr 基准=字段自身地址(mapped_file.h:43),非结构体首址;所有 readInt 偏移按此计算。

逐位等价(golden 覆盖,必须 bit-exact):
- darts 搜索的 {value,length} 结果集与顺序(commonPrefixSearch 按前缀长度升序;ExpandSearch 按先自身后 alphabet-BFS 序,含 512 截断);
- credibility 数值链:文件 float→double widen→逐边累加顺序(Table::Query BFS 按队列序累加,dict Peek 是 weight - log(1e8) + chunk.credibility)——同一算式 double 加法顺序影响末位,golden 比对权重必须同序;
- 建图三阶段结果:edges/vertices 集合、每边 spelling 集合、path type 传播、stale 裁剪结果、CheckOverlappedSpellings 标记、completion 边;
- interpreted_length 与返回值。

可行为等价(golden 不覆盖):
- tips 内容仅进 comment 展示,可不进对齐核心;
- map 迭代序在"结果先重排序再输出"的下游(chunk 堆排序 dictionary.cc:80-106、collector 按 end_pos 分桶)——Kotlin 用 SortedMap 保持同序最稳,个别处允许实现序差异(spellings 内 min-type 合并结果与序无关);
- DLOG 日志、if(false) 死块(可只留数据不留惩罚计算);
- canonicalizer/corrector:目标 schema 未启用时整个旁路可缺席(行为等价于"代码存在但不触发")。

实现难点排序(依据=语义分叉点数量与 golden 风险):
1. 建图算法本体(syllabifier.cc:31-321)——三阶段+顶点 min-type 丢弃+path max-type 传播+stale 裁剪交互,唯一语义分叉高发区,建议 golden 用 fuzz/abbrev 混合 schema 覆盖。
2. darts 解码与四原语——逻辑固定但位运算易错(label MSB、offset 8/0 变长移位),风险低但属地基,错一位全盘错。
3. prism/table 二进制布局解析——OffsetPtr 相对寻址、float credibility、List/Array 双形态,一次性工作,需用 C++ 侧 Dump 对拍。
4. ExpandSearch 复刻(completion+预测)——需精确 alphabet 序 BFS+512 截断,中低难度。
5. canonicalizer(仅当 schema 用 translator/canonicalize)——需移植 boost::regex 语义到 Kotlin regex,语法不完全兼容,中高风险,建议生成期检查 schema 再决定立项。
6. corrector——keyboard_map+traverse 变位技巧(corrector.cc:267-273),可缓;编译期 correction algebra 走 SpellingMap 已物化,无需额外文件(.correction.bin 构建被 #if 0,dict_compiler.cc:338-358)。

## Q7 与消费侧的衔接契约

消费侧只读 graph.indices、graph.interpreted_length(user_dictionary 不读 vertices),生产侧不变量四条。

Table::Query 精确消费方式(table.cc:572-631):
1. 守卫:start_pos < graph.interpreted_length(table.cc:575)。
2. BFS 队列 queue<{pos, TableQuery}> 从 (start_pos, 空查询) 起(:578-580)。
3. 每状态先 graph.indices.find(current_pos),miss 则 continue(:585-588)——**不读 edges/vertices**。
4. query.level()==kIndexCodeMaxLength(=3,vocabulary.h:27)时 Access(-1) 走 4+ 音节 tail 索引,结果挂 result[current_pos](:589-595)。
5. 否则遍历 indices[pos](按 syllable_id 升序),对每个 props:读 props->end_pos(前进依据)、props->credibility(累计)、props->type==kNormalSpelling(quality_len 记分 (end-start),table.cc:612-615);Access 产出该音节在 table trunk 索引的词条(credibility 传前缀和,:190-217),非空则挂 result[end_pos];若 end_pos < interpreted_length 且 Advance 成功则入队(:616-626,Backdate 复用同一查询对象)。
6. 上游 lookup_table(dictionary.cc:308-339):cr = initial_credibility + a.credibility();带 extra_code 的长词条用 match_extra_code 递归——同样只读 indices+props->end_pos(:114-145),取 max end_pos 的匹配。
7. 第二消费者 UserDictionary::DfsLookup:DFS 版同一契约(indices/end_pos/credibility/type,user_dictionary.cc:253-332,e->weight = log(...)+credibility @439)。

生产侧必须保证的不变量:
1. indices 完备:每条边的 (start, syllable_id) 都出现在 indices,且每个 props 携带正确 end_pos(Transpose 保证,:311-321)——消费侧唯一入口。
2. end_pos 与边终点一致且 ≤ interpreted_length;completion 边 end_pos=input.length=interpreted_length。
3. 非空 spelling 集:空集边建图时已擦除(syllabifier.cc:156-160),否则 Access 仍产出空 accessor 但白走队列。
4. 每位置 vertices[].type 语义(首访最小 type)虽不被 Table::Query 读,但被 stale 裁剪与 CheckOverlappedSpellings 间接决定图形状;script_translator 的 Syllabify/IsCorrection(script_translator.cc:359-415)直接读 edges——Kotlin 若只复刻到候选层可不暴露 edges,但 IsCorrection 等价判定需保留 edges。
5. credibility 累加起点一致:Table::Query 每音节累加从 0 起,initial_credibility 由调用方传(script_translator 传默认 0,dictionary.cc:341-345)。

## 已验证事实清单(含上文未展开项)

- gear/speller.cc 是按键 Processor,无建图职责(speller.cc:100-146)。
- 建图唯一生产入口:Syllabifier::BuildSyllableGraph,被 script_translator.cc:374-377 与 reverse_lookup_translator.cc:183 调用。
- 编译期 prism 构建链:dict_compiler.cc:298-376(读 speller/algebra@328→Projection@329-336→prism_->Build(*syllables,&script)@368)。
- Prism::Build 无 algebra 时键=音节表、有 algebra 时键=spelling 且 spelling_map[i]↔script 第 i 项(prism.cc:142-256);spelling_id=script 迭代序(map 字典序)。
- format="Rime::Prism/5.0"、version gate 5.0(prism.cc:32-33,110-115);spelling_map 自 v1.0 起存在(:128-130)。
- Table::Query 歧义惩罚在 if(false) 死块(table.cc:601-609)。
- corrector 恒为 NearSearchCorrector(corrector.cc:321),EditDistanceCorrector 构建被 #if 0(dict_compiler.cc:338-358,corrector.cc:304-320)。
- delimiters 默认 " "(translator_commons.cc:165-166),配置名 speller/delimiter 或 <ns>/delimiter(translator_commons.cc:119-120);enable_completion 默认 true(translator_commons.h:176);strict_spelling 默认 false(:177)。

## 推测(标注置信度)

- Path B 若以"schema 生成期展开 algebra"替代运行时 Projection:algebra 已物化进 prism.bin,**PoC 阶段完全不需要移植 calculus**,canonicalizer 只需检测目标 schema 是否含 translator/canonicalize(置信度:高,依据=Q4 证据链)。
- Script 迭代序即 spelling_id 分配序(map 字典序)在 golden 对齐时无关紧要,spelling_id 只在文件内部自洽使用(置信度:高)。

## 调查覆盖范围

已覆盖:algo/syllabifier.{h,cc}、algo/algebra.{h,cc}、algo/calculus.{h,cc}、algo/spelling.{h,cc}、dict/prism.{h,cc}、dict/mapped_file.h(布局)、dict/dictionary.cc、dict/table.cc(Query 链)、dict/user_dictionary.cc(消费面抽查)、dict/corrector.{h,cc}、dict/vocabulary.h、gear/speller.{h,cc}、gear/script_translator.cc(前 630 行)、include/darts.h(单元编码+三原语实现)、dict/dict_compiler.cc(prism 构建段)、common.h(容器 typedef)。未覆盖:reverse_lookup_translator 细节(仅确认调用点)、table.bin 二进制布局细节(消费侧既有 T0 报告范围)、chord_composer 的 algebra 用法(独立旁路,与音节图无关)。建议后续:table.bin syllabary 段的 mmap 解析规格(Q6 清单第 3 项的对拍底料)可另开一单。

SUMMARY: Q=7/7, facts=41, hypotheses=2
