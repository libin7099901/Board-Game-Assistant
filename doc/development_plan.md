# 安卓端核心模块原型开发计划

## 一、目标
完成动态加载引擎（Impeller优化）、插件管理面板（下载/卸载）功能开发，验证MinIO插件包下载解压至私有目录的流程。

## 二、任务分解
### 1. 动态加载引擎优化（周期：2周）
- 环境搭建：配置Flutter安卓开发环境，启用Impeller图形引擎（修改android/gradle.properties添加`flutter.impeller=true`）<mcfile name="gradle.properties" path="f:\workspace\桌游助手\android\gradle.properties"></mcfile>
- 性能测试：使用Flutter DevTools监控渲染帧率，对比Impeller启用前后的性能差异

### 2. 插件管理面板开发（周期：3周）
- UI设计：基于架构文档中的通用组件库，设计游戏列表页（RecyclerView展示）、下载进度条（ProgressBar）
- 功能实现：
  - 下载：调用Retrofit请求分发服务元数据，使用OkHttp下载插件包（存储路径：/data/data/com.xxx.xxx/plugins）
  - 卸载：删除对应插件目录并更新本地数据库记录

### 3. 流程验证（周期：1周）
- 测试用例：
  1. 从MinIO下载《卡坦岛》插件包（v1.0.0），校验签名后解压
  2. 卸载插件后检查私有目录残留文件
  3. 弱网环境下下载中断的恢复逻辑
- 兼容性测试：覆盖安卓10-14系统，验证存储权限（Android 11+使用SAF框架）