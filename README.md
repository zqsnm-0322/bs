# 颐康云·智慧养老院管理系统

> 基于 Spring Boot + Vue3 + 微信小程序 的养老机构综合管理解决方案

---

## 项目简介

**颐康云**是一款面向养老机构的智慧化管理平台，涵盖老人入住管理、健康评估、护理服务、家属互动、探访预约等核心业务场景，帮助养老机构实现数字化、精细化管理。

本系统由毕业设计「医疗风险预警系统」迁移改造而来，将原医疗场景的核心能力适配至养老护理领域。

---

## 技术栈

| 层级 | 技术选型 |
|------|---------|
| **后端** | Spring Boot 2.7.14 · MyBatis-Plus · MySQL 8.0 · Maven |
| **前端管理后台** | Vue 3 · TypeScript · Vite · Pinia · Element Plus · Axios |
| **微信小程序** | 微信原生框架 · WXML/WXSS · JavaScript |
| **构建工具** | pnpm（前端）· Maven（后端） |
| **开发语言** | Java 17 · TypeScript |

---

## 项目结构

```
bs/
├── backend/                # 后端服务（Spring Boot）
│   └── src/main/java/com/graduation/medical/
│       ├── controller/     # 控制器层
│       ├── entity/         # 实体类
│       ├── mapper/         # MyBatis-Plus Mapper
│       ├── service/        # 业务逻辑层
│       └── common/         # 公共组件（Result、异常处理等）
├── art-design-pro-main/    # 前端管理后台（Vue3 + TypeScript）
│   └── src/
│       ├── views/          # 页面组件
│       ├── api/            # API 接口封装
│       └── router/         # 路由配置
├── miniprogram/            # 微信小程序端
│   └── pages/              # 页面目录（首页/老人/护理员/健康等）
├── sql/                    # 数据库脚本
│   └── medical_to_elderly.sql   # 系统改造 SQL 脚本
└── uploads/                # 文件上传存储目录
```

---

## 核心功能

### 老人管理
- **入住登记**：自动生成入住编号（YZ + 年份 + 序号），录入老人基本信息、健康状况、过敏史、饮食禁忌等
- **老人档案**：整合基本信息、家属信息、健康检查记录、风险评估、护理任务的多 Tab 档案页
- **离院管理**：支持办理离院，释放床位并记录离院原因
- **智能筛选**：按姓名、房间床位、护理区、健康等级等多维度检索

### 护理区管理
- 四大护理区：**自理区 / 半护区 / 全护区 / 失智区**
- 床位管理：房间类型（单人/双人/多人）、楼层、床位状态（空闲/已入住/维护中）
- 护理区分布看板

### 健康检查
- 定期体检记录：血压、心率、体温、血糖、体重、BMI、血氧等
- BMI 自动计算
- 历史检查趋势追踪

### 家属管理
- 多家属信息录入：关系、联系方式、住址
- 主要联系人标识
- 探访频率偏好设置

### 探访预约
- 家属在线提交探访申请
- 养老院审批流程（待确认 → 已批准 / 已拒绝）
- 探访时间、人数、特殊备注管理

### 风险等级评估
- 基础风险：跌倒 / 压疮 / VTE 血栓 / 脑卒中
- 养老扩展：认知障碍（MMSE/MoCA）/ 营养不良（MNA-SF）/ 谵妄（3D-CAM）/ 抑郁（GDS-15）

### 权限体系
| 角色 | 权限说明 |
|------|---------|
| ADMIN（管理员） | 全量数据访问 |
| DIRECTOR（院长） | 全量数据访问 |
| NURSE（护理员） | 仅查看所属护理区的老人 |
| CAREGIVER（照护员） | 仅查看负责老人 |

---

## 快速开始

### 环境要求
- JDK 17+
- Node.js 20+ / pnpm 8+
- MySQL 8.0+
- Maven 3.6+

### 1. 初始化数据库
```bash
# 导入 SQL 脚本
mysql -u root -p < sql/medical_to_elderly.sql
```

### 2. 启动后端
```bash
cd backend
mvn spring-boot:run
# 服务启动于 http://localhost:8080
```

### 3. 启动前端管理后台
```bash
cd art-design-pro-main
pnpm install
pnpm dev
# 访问 http://localhost:5173
```

### 4. 微信小程序
使用微信开发者工具打开 `miniprogram/` 目录即可运行。

---

## 数据库概览

| 表名 | 说明 |
|------|------|
| `biz_patient` | 入住老人信息（主表，已改造） |
| `biz_bed` | 居住床位信息 |
| `sys_user` | 系统用户/员工信息 |
| `biz_family_member` | 家属信息 |
| `biz_health_checkup` | 健康检查记录 |
| `biz_visit_appointment` | 探访预约 |

---

## 项目说明

- **改造方案文档**：详见 `养老系统改造方案.md`
- **系统图稿**：详见 `养老院管理系统图稿.vsdx`
- **毕业设计论文**：详见 `养老院管理系统毕业设计论文.docx`

---

## 作者

毕业设计项目 · 颐康云智慧养老服务平台
