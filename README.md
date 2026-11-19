# 英语学习 App · 结课作业

背单词喂养宠物的全栈小应用：Android 客户端 + Spring Boot 业务服务 + Python LangGraph AI 服务。
**三个子项目各自独立**，可分别用 Android Studio / IDEA / PyCharm 打开对应目录，构建互不干扰。

## 一句话架构

```
Android(Kotlin) --HTTP--> Spring Boot :8080 --HTTP--> Python FastAPI :8081 --HTTPS--> DeepSeek
                                |                          |
                            MySQL 7 张表              LangGraph 四步图 / LangChain 翻译链
```

## 目录

| 目录 | 内容 | 独立打开方式 |
|---|---|---|
| `android-app/` | Android 工程（Kotlin + XML + Retrofit） | Android Studio 打开这一层 |
| `spring-server/` | Spring Boot 业务服务（JWT、MyBatis-Plus、13 接口） | IDEA 打开 `pom.xml` |
| `python-ai/` | FastAPI + LangGraph 四步图 + LangChain 翻译链 | PyCharm 指向 `requirements.txt` |
| `db/` | `schema.sql`（7 表）、`seed.sql`（200 词 + 6 形象）、`gen_seed.py` | 共享资源 |
| `tools/` | `smoke.py` 接口冒烟、`gen_characters.py` 形象占位图 | 共享资源 |
| `doc/` | 方案、开发流程、AI 节点图、测试报告、主题原型、截图 | 共享资源 |
| `.workbuddy/skills/english-app-dev/` | 续开发手册（SKILL + 5 份 references） | 共享资源 |

## 快速启动（4 步，按顺序）

```bash
# 0 前置：MySQL 密码与 Python
export MYSQL_PWD='你的密码'

# 1 数据库（首次）
mysql -u root -e "CREATE DATABASE IF NOT EXISTS english_app DEFAULT CHARSET utf8mb4;"
mysql -u root english_app < db/schema.sql
mysql -u root english_app < db/seed.sql

# 2 AI 服务 :8081（先起它，否则 AI 功能全是提示语）
cd python-ai
python -m venv .venv && .venv/Scripts/python.exe -m pip install -r requirements.txt   # 首次
.venv/Scripts/python.exe -m uvicorn app.main:app --port 8081
cd ..

# 3 业务服务 :8080（必须 unset SERVER__PORT，否则端口会被环境变量改掉）
cd spring-server
export MYSQL_PASSWORD='你的密码'
unset SERVER__PORT
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
cd ..

# 4 客户端（另开一个终端）
cd android-app && ./gradlew installDebug
```

演示账号：`demo01 / demo2026`（**单账号，App 不提供注册入口**）。没有 API Key 也能跑——`python-ai` 会自动切 Mock 模型。
跑完测试想恢复初始状态：`mysql -u root english_app < db/reset_demo.sql`。

## 测试

```bash
(cd python-ai && .venv/Scripts/python.exe -m pytest -q)   # AI 单测 5 个
(cd spring-server && mvn -q test)                          # 后端单测 5 个
(python tools/smoke.py)                                    # 接口冒烟 25 条（需 8080/8081 已启动，单账号可重复跑）
(cd android-app && ./gradlew assembleDebug)                # 客户端构建
```

当前状态：单元测试 16/16（Java 8 + Python 8）、接口冒烟 25/25、客户端构建通过、模拟器端到端全通，详见 `doc/测试报告.md`。

## 功能进度（对照 `doc/整改方案.md`）

| 期 | 内容 | 状态 |
|---|---|---|
| P0 | 食物表/背包表 + 买食物 + 按食物喂食 + 4 个新接口 | ✅ 已完成 |
| P1 | 新首页（10 格饥饿/经验/等级/形象名）+ 商城 + 背包 + 设置 + 学习中心 | ✅ 已完成 |
| P2 | 背单词三题型（认识 / 缺空 / 完整）+ 答题消耗饥饿 | ✅ 已完成 |
| P3 | 作文打分 rubric + 扣分明细；作文生成（第二张 LangGraph 图） | ✅ 已完成 |
| P4 | 主题 B 令牌、手写体、容器色统一、首页留白重排 | ✅ 已完成 |

## 文档入口

- 方案与答辩口径：`doc/英语学习App-结课作业版.md`
- 完整开发流程（阶段/验收/坑）：`doc/开发流程.md`
- LangGraph / LangChain 节点图：`doc/AI-节点图.md`
- 测试结果与未完成项：`doc/测试报告.md`
