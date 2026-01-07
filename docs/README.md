# CloudLib 响应式 UI 系统文档

## 文档目录

这是 CloudLib 响应式 UI 系统的完整技术文档。文档按模块划分，每个模块都包含详细的设计理念、API 说明和实际案例。

---

## 📚 文档结构

### 第一部分：基础概念

| 文档 | 描述 |
|------|------|
| [01-introduction.md](./01-introduction.md) | 系统介绍、设计背景、与其他框架对比 |
| [02-reactive-primitives.md](./02-reactive-primitives.md) | Signal、Computed、Effect 响应式原语 |
| [03-tracker.md](./03-tracker.md) | 依赖追踪系统原理与实现 |

### 第二部分：组件系统

| 文档 | 描述 |
|------|------|
| [04-component.md](./04-component.md) | Component 类型、生命周期、状态管理 |
| [05-component-context.md](./05-component-context.md) | Hook API 详解（signal、computed、effect 等） |
| [06-render-dsl.md](./06-render-dsl.md) | 声明式 UI DSL 语法与使用 |

### 第三部分：元素树系统

| 文档 | 描述 |
|------|------|
| [07-render-node.md](./07-render-node.md) | RenderNode 类型与创建 |
| [08-element-tree.md](./08-element-tree.md) | ElementTree、Element 层次结构、Reconciliation |
| [09-fine-grained-reactivity.md](./09-fine-grained-reactivity.md) | 细粒度响应式原理与优化 |

### 第四部分：高级主题

| 文档 | 描述 |
|------|------|
| [10-style-system.md](./10-style-system.md) | 样式系统设计与使用 |
| [11-conditional-list.md](./11-conditional-list.md) | 条件渲染、列表渲染、Key 机制 |
| [12-best-practices.md](./12-best-practices.md) | 最佳实践、常见错误、性能优化 |

### 附录

| 文档 | 描述 |
|------|------|
| [appendix-a-api-reference.md](./appendix-a-api-reference.md) | 完整 API 参考 |
| [appendix-b-examples.md](./appendix-b-examples.md) | 完整示例代码集合 |

---

## 🚀 快速开始

如果你是第一次接触这个系统，建议按以下顺序阅读：

1. **[01-introduction.md](./01-introduction.md)** - 了解系统全貌
2. **[02-reactive-primitives.md](./02-reactive-primitives.md)** - 掌握核心响应式原语
3. **[04-component.md](./04-component.md)** - 学习组件开发
4. **[06-render-dsl.md](./06-render-dsl.md)** - 掌握 UI 构建语法
5. **[12-best-practices.md](./12-best-practices.md)** - 了解最佳实践

---

## 📋 版本信息

- **框架版本**: CloudLib Reactive UI 1.0
- **文档版本**: 1.0
- **最后更新**: 2026年1月6日
- **目标平台**: Minecraft NeoForge

