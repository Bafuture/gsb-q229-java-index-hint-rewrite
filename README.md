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

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的查询经常选错索引导致全表扫描，希望能根据规则重写查询并给出索引建议。请从零实现一个索引提示与查询重写组件。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 用结构化对象表示查询条件（字段、操作符、值）与可用索引清单；2) 支持重写规则至少三种：条件顺序调整以便用上组合索引、把函数包裹的字段条件改写为范围条件、把 OR 条件改写为可分别使用索引的形式；3) 支持索引选择：为每个候选索引估算可过滤比例，选出代价最低的一个并给出选择理由；4) 支持强制提示：调用方可指定必须使用的索引，若该索引无法覆盖条件要报错说明原因；5) 支持重写前后等价性验证：对同一批数据执行重写前后的查询，结果必须一致；6) 支持输出重写前后的条件与选中的索引，便于排查；7) 提供统计：规则命中次数、可选索引数与被排除的索引及原因；8) 测试覆盖三类重写规则、索引选择与理由、强制提示报错、等价性验证与输出内容；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。

## 组件使用说明

核心入口为 `com.example.gsb.indexhint.QueryRewriteEngine`：

```java
QueryRewriteEngine engine = new QueryRewriteEngine();
RewriteRequest request = RewriteRequest.of(
        AndCondition.of(
                new FunctionCondition("date", "created_at", Operator.EQ, "2024-01-15"),
                Comparison.eq("status", "PAID")),
        List.of(IndexDefinition.of("idx_status_created", "status", "created_at")));

RewriteResult result = engine.rewrite(request);          // 可 .forceIndex("idx_x") 强制索引
System.out.println(result.report().format());            // 重写前后条件 + 选中索引 + 选择理由
System.out.println(result.statistics());                 // 规则命中次数 / 可选索引数 / 被排除索引及原因
result.verifyEquivalence(dataset);                       // 在同一批数据上验证重写前后结果一致
```

- 条件模型：`Comparison`（字段/操作符/值）、`FunctionCondition`、`AndCondition`、`OrCondition`、`UnionCondition`。
- 内置规则：`FunctionToRangeRule`（`DATE`/`YEAR` 等值条件转范围）、`OrToUnionRule`（各分支均可用索引时 OR 转 UNION）、`CompositeIndexOrderRule`（按组合索引列序调整条件顺序）。
- 索引选择：`IndexSelector` 按最左前缀匹配估算过滤比例与代价，输出选择理由；强制索引不可用时抛出 `IndexHintException` 并说明原因。
