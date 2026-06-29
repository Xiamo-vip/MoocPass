<div align="center">

# MoocPass (墨客通)

**在线网课挂机与 AI 答题系统**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue.js-3.x-4FC08D.svg?style=flat&logo=vuedotjs)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-6.x-646CFF.svg?style=flat&logo=vite)](https://vitejs.dev/)
[![Java](https://img.shields.io/badge/Java-17-ED8B00.svg?style=flat&logo=openjdk)](https://www.oracle.com/java/)
[![Python](https://img.shields.io/badge/Python-3.10+-3776AB.svg?style=flat&logo=python)](https://www.python.org/)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg?style=flat)](LICENSE)

[功能特性](#-功能特性) • [系统架构](#-系统架构) • [快速开始](#-快速开始) • [项目结构](#-项目结构) • [贡献指南](#-贡献者与社区)

</div>

---

## 📖 简介

**MoocPass** 是一个在线网课管理与自动化挂机系统。系统采用前后端分离架构，前端提供平台控制台页面，后端基于 Java 实现网课平台协议对接，并集成 Python OCR 验证码解码服务与 AI 大模型答题功能。

---

## ✨ 功能特性

### 平台对接与挂机处理
* **超星学习通**：支持账号密码与 Token 验证，解析章节卡片，处理视频播放进度上报与试卷答题。
* **职教云 / AI 优课**：提供独立控制台页面，对接 `ai.icve.com.cn`（AI 优课）与 `mooc.icve.com.cn`（传统 MOOC / 资源库）接口，支持课程检索去重、章节结构解析与打卡记录提交。

### 验证码识别 (CaptchaDecoder)
* 基于 Python `ddddocr` 库提供验证码识别服务，处理超星平台安全验证码（9010 拦截）验证请求。

### AI 智能答题
* 对接大语言模型 API，提供试卷题目解析、答案生成与覆盖率提交控制。
* **智慧职教自动答题开发中**：智慧职教（职教云/AI优课）平台的自动答题与考试处理功能目前正处于开发状态。

---

## 🏗️ 系统架构

```
                     ┌─────────────────────────────────────────┐
                     │              Vue 3 前端页面             │
                     │   (超星控制台 / 职教云控制台 / 任务日志)  │
                     └────────────────────┬────────────────────┘
                                          │  RESTful API
                                          ▼
                     ┌─────────────────────────────────────────┐
                     │          Spring Boot 3 Java 后端        │
                     │  (PlatformRegistry / TaskExecutorPool)  │
                     └──────────┬───────────────────┬──────────┘
                                │                   │
             ┌──────────────────┘                   └──────────────────┐
             ▼                                                         ▼
┌─────────────────────────┐                               ┌─────────────────────────┐
│  Chaoxing / Zhy 平台模块 │                               │  CaptchaDecoder 微服务   │
│ (OkHttp 4 + Jsoup 爬虫)  │                               │   (Python + ddddocr)    │
└─────────────────────────┘                               └─────────────────────────┘
```

---

## 🛠️ 技术栈

| 领域 | 核心技术 / 框架 | 说明 |
| :--- | :--- | :--- |
| **前端 (Web)** | Vue 3 + Vite + Vue Router | 页面视图构建与路由管理 |
| ** UI 样式** | CSS3 + Lucide Icons | 界面布局与图标支持 |
| **后端 (Server)** | Spring Boot 3 + Java 17 | 业务逻辑处理与任务调度调度引擎 |
| **数据持久化** | MyBatis-Plus + H2 / MySQL | 任务数据与配置存储 |
| **网络通信** | OkHttp 4 + Jsoup | Cookie 管理、网络重试与 HTML 解析 |
| **验证码识别** | Python 3 + Flask + ddddocr | 验证码图片识别与求解服务 |

---

## 🚀 快速开始

### 环境要求
* **Node.js**: `>= 18.0.0`
* **JDK**: `>= 17`
* **Python**: `>= 3.10` (用于验证码识别服务)

### 1. 启动验证码识别服务 (可选)
```bash
cd CaptchaDecoder
pip install -r requirements.txt
python main.py
```
*服务运行在 `http://localhost:5000`*

### 2. 编译并启动后端服务 (Server)
```bash
cd Server
# Windows 平台编译
.\gradlew.bat build -x test
# 启动服务
.\gradlew.bat bootRun
```
*后端 API 服务运行在 `http://localhost:8080`*

### 3. 启动前端项目 (Web)
```bash
cd Web
npm install
npm run dev
```
*访问 `http://localhost:5173` 进入系统界面*

---

## 📁 项目结构

```
MoocPass/
├── Server/                   # Java 后端工程
│   ├── src/main/java/top/xiamoi/moocpass/
│   │   ├── controller/      # API 控制层
│   │   ├── entity/          # 实体类定义
│   │   ├── platform/        # 平台适配器实现 (Chaoxing / Zhy)
│   │   ├── solver/          # AI 答题服务实现
│   │   ├── task/            # 任务调度与日志处理
│   │   └── util/            # 网络工具类
│   └── build.gradle
├── Web/                      # Vue 3 前端工程
│   ├── src/
│   │   ├── components/      # 页面组件
│   │   ├── pages/           # 控制台页面
│   │   └── router/          # 路由配置
│   └── package.json
└── CaptchaDecoder/           # Python 验证码识别服务
    ├── decoders/            # 解码器实现
    ├── services/            # 验证码求解服务
    └── main.py
```

---

## 💡 使用流程

1. **凭证配置**：在系统中配置网课平台凭证（超星学习通账号密码或 Cookies，职教云绑定 Token）。
2. **获取课程**：进入平台控制台点击“同步课程”，获取账号下的课程列表。
3. **创建任务**：选择目标课程，设置播放倍速与答题参数后启动任务。
4. **运行监控**：在控制台日志栏中查看任务执行进度与上报记录。

---

## 🤝 贡献者与社区

欢迎所有开发者与贡献者参与完善项目！无论是修复 Bug、优化性能、完善文档，还是共同参与开发**智慧职教自动答题**等新功能，都非常欢迎提交 Issue 或 Pull Request 参与贡献。

---

## 📄 许可说明

本项目基于 [GNU General Public License v3.0 (GPL-3.0)](LICENSE) 开源。仅供个人学习与技术研究使用。
