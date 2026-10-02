# 索引提示与查询重写组件

Pair-wise GSB 标注任务仓库（第 15 批 / 229）。

| 项目 | 内容 |
|------|------|
| 任务类型 | Feature 迭代 |
| 任务难度 | 困难 |
| 语言/框架 | Java, Maven, JUnit 5 |
| 环境可复现等级 | 无外部依赖 |
| 构建方式 | Maven（含 mvnw wrapper，无需本机安装 Maven） |

> 本仓库是**初始环境快照**：只有工程骨架，不含任何实现代码。
> 分支说明：`main` 为初始环境；`A`、`B` 为两次独立执行各自的工作分支，均从 `main` 的同一个提交拉出。

## 运行方式

```bash
./mvnw -q verify
```

## 实现说明

组件位于 `com.example.gsb.indexhint`，入口为 `QueryOptimizer`。

- 结构化模型：`Query` / `FieldCondition` / `FunctionCondition` / `OrCondition` / `Index` / `TableStats`。
- 重写规则（`RewriteRule`）：
  - `FunctionToRangeRule`：`DATE(c) = '2024-01-15'`、`YEAR(c) = 2024` 改写为半开区间范围条件；
  - `OrToUnionRule`：可分别命中索引的 OR 条件拆分为 `UNION ALL` 分支（按索引前缀判定）；
  - `CompositeIndexReorderRule`：按组合索引列顺序前置 AND 条件。
- 索引选择：`IndexSelector` 基于 `TableStats` 的基数与 min/max 估算每列过滤比例，连乘得到扫描行数，选代价最低者；不可用索引以原因排除。
- 强制提示：`optimize(..., forcedIndex)`；索引不存在或无法覆盖任何条件时抛 `IndexHintException` 并说明原因。
- 等价性验证：`optimizeWithSample(...)` 传入样本行，`EquivalenceChecker` 对重写前后实际执行结果（`RowExecutor`）做集合比对。
- 排查输出：`OptimizationResult.report()` 输出原始/重写后条件、各分支选中索引及理由、规则命中次数、可选索引数、被排除索引及原因、等价性验证结论。

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的查询经常选错索引导致全表扫描，希望能根据规则重写查询并给出索引建议。请从零实现一个索引提示与查询重写组件。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 用结构化对象表示查询条件（字段、操作符、值）与可用索引清单；2) 支持重写规则至少三种：条件顺序调整以便用上组合索引、把函数包裹的字段条件改写为范围条件、把 OR 条件改写为可分别使用索引的形式；3) 支持索引选择：为每个候选索引估算可过滤比例，选出代价最低的一个并给出选择理由；4) 支持强制提示：调用方可指定必须使用的索引，若该索引无法覆盖条件要报错说明原因；5) 支持重写前后等价性验证：对同一批数据执行重写前后的查询，结果必须一致；6) 支持输出重写前后的条件与选中的索引，便于排查；7) 提供统计：规则命中次数、可选索引数与被排除的索引及原因；8) 测试覆盖三类重写规则、索引选择与理由、强制提示报错、等价性验证与输出内容；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。
