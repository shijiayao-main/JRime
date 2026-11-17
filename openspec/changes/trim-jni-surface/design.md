# Design

> 分析与证据链见 doc/trim-plan.md(每项的 file:line 与核查命令),本文件只记实施决策。

## 决策 1:三层连删,一次 commit

Kotlin external fun 声明、RimeApi/Rime 接口层、C++ JNI 函数在同一 commit 内删除。JNI 按名绑定,只删一侧不会编译失败:删 C++ 留 Kotlin 声明会在运行时 UnsatisfiedLinkError,反向只是死符号——编译器帮不上忙,必须人工保证两侧同删,并以 harness 冒烟(完整会话)+ 单测兜底。

## 决策 2:SwitcherSettings 类方法随 JNI 函数一起删

levers.cc 的 availableSchemas() / selectedSchemas() / selectSchemas() 三个类方法只被三个死 JNI 函数调用,删函数后即成死方法,一并删;userDictList / backupUserDict / restoreUserDict / exportUserDict / importUserDict 有 UserDictManager 调用方,保留。

## 决策 3:custom_predict_rules.yaml 删除

predict.db 未内置(roadmap 决策 2),规则库当前惰性且唯一引用为注释;日后经资产包通道启用 predict 规则时,该文件应随资产包重新进入,而非留在 shared/ 误导。

## Open Questions

- 无。裁剪范围已在 doc/trim-plan.md 与调用点核查中定死。
