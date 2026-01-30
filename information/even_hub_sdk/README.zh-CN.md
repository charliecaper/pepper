# @evenrealities/even_hub_sdk

[![npm version](https://img.shields.io/npm/v/@evenrealities/even_hub_sdk.svg)](https://www.npmjs.com/package/@evenrealities/even_hub_sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Node.js Version](https://img.shields.io/badge/node-%3E%3D20.0.0-brightgreen.svg)](https://nodejs.org/)

> TypeScript SDK for WebView developers to communicate with Even App

[English](README.md) | **中文**

## 📑 目录

- [简介](#-简介)
- [安装](#-安装)
- [快速开始](#-快速开始)
  - [基础使用](#基础使用)
  - [设备状态监听](#设备状态监听)
  - [创建眼镜UI](#创建眼镜ui)
  - [事件监听](#事件监听)
- [API 文档](#-api-文档)
  - [EvenAppBridge](#evenappbridge)
  - [数据模型](#数据模型)
  - [枚举类型](#枚举类型)
- [高级用法](#-高级用法)
- [注意事项](#-注意事项)
- [贡献](#-贡献)
- [联系方式](#-联系方式)
- [更新日志](#-更新日志)
- [许可证](#-许可证)

## 📖 简介

`@evenrealities/even_hub_sdk` 是一个专为 Even App 生态系统设计的 TypeScript SDK，用于在 WebView 环境中实现 Web 页面与 Even App 之间的双向通信。该 SDK 提供了类型安全的桥接接口，支持设备信息管理、用户信息获取、本地存储操作以及 EvenHub 协议的核心功能调用。

### 核心特性

- 🔌 **统一的桥接封装**：提供 `EvenAppBridge` 类，实现 Web 与 App 之间的类型安全通信
- 📱 **设备管理**：获取设备信息、监听设备状态变化（连接状态、电量、佩戴状态等）
- 👤 **用户信息**：获取当前登录用户信息
- 💾 **本地存储**：提供键值对存储接口，数据持久化到 App 端
- 🎯 **EvenHub 协议支持**：完整支持 EvenHub 协议的核心调用（JSON 字段映射）
- 📡 **事件监听**：支持设备状态变化和 EvenHub 事件的实时推送监听
- 🛡️ **类型安全**：完整的 TypeScript 类型定义，提供良好的开发体验
- 🔄 **自动初始化**：SDK 自动初始化桥接，无需手动配置

## 📦 版本信息

- **当前版本**: `0.0.6`
- **Node.js 要求**: `^20.0.0 || >=22.0.0`
- **TypeScript 支持**: 完整支持，包含类型定义文件

## 🚀 安装

使用 npm 安装：

```bash
npm install @evenrealities/even_hub_sdk
```

或使用 yarn：

```bash
yarn add @evenrealities/even_hub_sdk
```

或使用 pnpm：

```bash
pnpm add @evenrealities/even_hub_sdk
```

## ⚡ 快速开始

### 基础使用

```typescript
import { waitForEvenAppBridge } from '@evenrealities/even_hub_sdk';

// 等待桥接初始化完成
const bridge = await waitForEvenAppBridge();

// 获取用户信息
const user = await bridge.getUserInfo();
console.log('User:', user.name);

// 获取设备信息
const device = await bridge.getDeviceInfo();
console.log('Device Model:', device?.model);

// 本地存储操作
await bridge.setLocalStorage('theme', 'dark');
const theme = await bridge.getLocalStorage('theme');
```

### 设备状态监听

```typescript
import { waitForEvenAppBridge, DeviceConnectType } from '@evenrealities/even_hub_sdk';

const bridge = await waitForEvenAppBridge();

const unsubscribe = bridge.onDeviceStatusChanged((status) => {
  if (status.connectType === DeviceConnectType.Connected) {
    console.log('设备已连接!', status.batteryLevel);
  }
});

// unsubscribe();
```

### 创建眼镜UI

> ⚠️ **重要提示**：创建自定义眼镜UI时，**必须**先调用 `createStartUpPageContainer`，然后才能进行其他UI操作。

```typescript
import {
  waitForEvenAppBridge,
  CreateStartUpPageContainer,
  ListContainerProperty,
  TextContainerProperty,
} from '@evenrealities/even_hub_sdk';

const bridge = await waitForEvenAppBridge();

// 创建容器
const listContainer: ListContainerProperty = {
  xPosition: 100,
  yPosition: 50,
  width: 200,
  height: 150,
  containerID: 1,
  containerName: 'list-1',
  itemContainer: {
    itemCount: 3,
    itemName: ['项目 1', '项目 2', '项目 3'],
  },
  isEventCapture: 1, // 只能有一个容器的 isEventCapture=1
};

const textContainer: TextContainerProperty = {
  xPosition: 100,
  yPosition: 220,
  width: 200,
  height: 50,
  containerID: 2,
  containerName: 'text-1',
  content: '你好世界',
  isEventCapture: 0,
};

// 创建启动页（最多4个容器）
const result = await bridge.createStartUpPageContainer({
  containerTotalNum: 2, // 最大值：4
  listObject: [listContainer],
  textObject: [textContainer],
});

if (result === 0) {
  // 如需要，更新图片数据
  // await bridge.updateImageRawData({ ... });
  
  // 如需要，更新文本内容
  // await bridge.textContainerUpgrade({ ... });
}
```

### 事件监听

```typescript
const bridge = await waitForEvenAppBridge();

const unsubscribe = bridge.onEvenHubEvent((event) => {
  if (event.listEvent) {
    console.log('列表选中:', event.listEvent.currentSelectItemName);
  } else if (event.textEvent) {
    console.log('文本事件:', event.textEvent);
  } else if (event.sysEvent) {
    console.log('系统事件:', event.sysEvent.eventType);
  }
});

// unsubscribe();
```

## 📚 API 文档

### EvenAppBridge

主要的桥接类，提供与 Even App 通信的所有方法。

#### 开始使用

##### 获取实例

```typescript
import { EvenAppBridge, waitForEvenAppBridge } from '@evenrealities/even_hub_sdk';

// 方式 1: 等待桥接就绪（推荐）
const bridge = await waitForEvenAppBridge();

// 方式 2: 直接获取单例（需要确保已初始化）
const bridge = EvenAppBridge.getInstance();
```

#### 基础信息方法

##### `getUserInfo(): Promise<UserInfo>`

获取当前登录用户信息。

**返回值**: `Promise<UserInfo>`

**示例**:
```typescript
const user = await bridge.getUserInfo();
console.log(user.name);
console.log(user.uid);
console.log(user.avatar);
console.log(user.country);
```

##### `getDeviceInfo(): Promise<DeviceInfo | null>`

获取设备信息（眼镜/戒指信息）。

**返回值**: `Promise<DeviceInfo | null>`

**示例**:
```typescript
const device = await bridge.getDeviceInfo();
if (device) {
  console.log('Model:', device.model);
  console.log('SN:', device.sn);
  console.log('Status:', device.status);
}
```

##### `setLocalStorage(key: string, value: string): Promise<boolean>`

设置本地存储值。

**参数**:
- `key: string` - 存储键名
- `value: string` - 存储值

**返回值**: `Promise<boolean>` - 操作是否成功

**示例**:
```typescript
await bridge.setLocalStorage('theme', 'dark');
await bridge.setLocalStorage('language', 'zh-CN');
```

##### `getLocalStorage(key: string): Promise<string>`

获取本地存储值。

**参数**:
- `key: string` - 存储键名

**返回值**: `Promise<string>` - 存储的值，不存在时返回空字符串

**示例**:
```typescript
const theme = await bridge.getLocalStorage('theme');
const language = await bridge.getLocalStorage('language');
```

#### 事件监听方法

##### `onDeviceStatusChanged(callback: (status: DeviceStatus) => void): () => void`

监听设备状态变化事件。

**参数**:
- `callback: (status: DeviceStatus) => void` - 状态变化时的回调函数

**返回值**: `() => void` - 取消监听的函数

**示例**:
```typescript
const unsubscribe = bridge.onDeviceStatusChanged((status) => {
  console.log('Connect Type:', status.connectType);
  console.log('Battery Level:', status.batteryLevel);
  console.log('Is Wearing:', status.isWearing);
  console.log('Is Charging:', status.isCharging);
});

// 取消监听
unsubscribe();
```

#### EvenHub API 接口

以下接口用于与 EvenHub 设备进行通信。

> **坐标系统说明**：眼镜画布使用坐标系统，原点 (0, 0) 位于**左上角**。X 轴方向**向右**（正值向右递增），Y 轴方向**向下**（正值向下递增）。

##### `createStartUpPageContainer(container: CreateStartUpPageContainer): Promise<StartUpPageCreateResult>`

创建启动页容器。**此方法仅在首次启动眼镜UI时必须调用一次**，之后不能再调用（即使调用也不会生效）。

> **重要说明**：
> - 创建多个容器时，**必须且仅能有一个**容器的 `isEventCapture=1`（其他容器必须为 `0`）
> - `containerTotalNum` 的**最大值为 4** - 最多只能创建 4 个容器（单一或多种混合类型）
> - 图片容器创建成功后需要调用 `updateImageRawData` 来显示实际内容（详见下方说明）

**参数**:
- `container: CreateStartUpPageContainer` - 容器配置对象

**返回值**: `Promise<StartUpPageCreateResult>` - 创建结果
- `0` = success（成功）
- `1` = invalid（无效）
- `2` = oversize（超出大小限制）
- `3` = outOfMemory（内存不足）

**示例**:
```typescript
import {
  CreateStartUpPageContainer,
  ListContainerProperty,
  TextContainerProperty,
  ImageContainerProperty,
  ListItemContainerProperty,
} from '@evenrealities/even_hub_sdk';

// 示例：创建一个包含列表、文本和图片的容器
const listContainer: ListContainerProperty = {
  xPosition: 100,
  yPosition: 50,
  width: 200,
  height: 150,
  borderWidth: 2,
  borderColor: 5,
  borderRdaius: 5,
  paddingLength: 10,
  containerID: 1,
  containerName: 'list-1',
  itemContainer: {
    itemCount: 3,
    itemWidth: 0, // 0 = 自动填充
    isItemSelectBorderEn: 1,
    itemName: ['项目 1', '项目 2', '项目 3'],
  },
  isEventCapture: 1,
};

const textContainer: TextContainerProperty = {
  xPosition: 100,
  yPosition: 220,
  width: 200,
  height: 50,
  borderWidth: 1,
  borderColor: 0,
  borderRdaius: 3,
  paddingLength: 5,
  containerID: 2,
  containerName: 'text-1',
  content: '你好世界',
  isEventCapture: 0,
};

const imageContainer: ImageContainerProperty = {
  xPosition: 320,
  yPosition: 50,
  width: 100,
  height: 80,
  containerID: 3,
  containerName: 'img-1',
};

const container: CreateStartUpPageContainer = {
  containerTotalNum: 3,
  listObject: [listContainer],
  textObject: [textContainer],
  imageObject: [imageContainer],
};

const result = await bridge.createStartUpPageContainer(container);
if (result === 0) {
  console.log('Container created successfully');
  
  // 如果有图片容器，创建成功后需要立即调用 updateImageRawData 来显示图片内容
  await bridge.updateImageRawData({
    containerID: 3,
    containerName: 'img-1',
    imageData: [/* 图片数据 */],
  });
} else {
  console.error('Failed to create container:', result);
}
```

> **图片容器说明**（适用于 `createStartUpPageContainer` 和 `rebuildPageContainer`）：与其他容器类型（列表和文本）不同，图片容器在创建时不需要提供数据。图片容器创建成功后，只会占据屏幕上的一个占位位置。你必须调用 `updateImageRawData` 来刷新视图，才会显示实际的图片内容。

**参数要求**:
- 详细参数约束请参考 [容器属性模型](#容器属性模型) 部分

##### `rebuildPageContainer(container: RebuildPageContainer): Promise<boolean>`

重建页面容器。用于更新当前页面或创建新页面。

> **重要说明**：
> - 此方法与 `createStartUpPageContainer` 在功能上完全一致，参数结构相同
> - **责任分工**：`createStartUpPageContainer` 仅在首次启动眼镜UI时必须调用一次，之后不能再调用；所有后续的页面更新或新页面创建都必须使用 `rebuildPageContainer`
> - 关于参数约束、容器配置、示例代码等详细信息，请参考 [`createStartUpPageContainer`](#createstartuppagecontainercontainer-createstartuppagecontainer-promisestartuppagecreateresult) 部分

**参数**:
- `container: RebuildPageContainer` - 容器配置对象（与 `CreateStartUpPageContainer` 结构相同）

**返回值**: `Promise<boolean>` - 操作是否成功

**示例**:
```typescript
import { RebuildPageContainer } from '@evenrealities/even_hub_sdk';

const container: RebuildPageContainer = {
  containerTotalNum: 2,
  listObject: [/* ... ListContainerProperty[] */],
  textObject: [/* ... TextContainerProperty[] */],
  imageObject: [/* ... ImageContainerProperty[] */],
};

const success = await bridge.rebuildPageContainer(container);
if (success) {
  // 如果有图片容器，重建成功后需要调用 updateImageRawData 来显示图片内容
  await bridge.updateImageRawData({
    containerID: 3,
    containerName: 'img-1',
    imageData: [/* 图片数据 */],
  });
}
```

**参数要求**:
- 与 `createStartUpPageContainer` 相同，详细参数约束请参考 [`createStartUpPageContainer`](#createstartuppagecontainercontainer-createstartuppagecontainer-promisestartuppagecreateresult) 部分

##### `updateImageRawData(data: ImageRawDataUpdate): Promise<ImageRawDataUpdateResult>`

更新图片原始数据。

**参数**:
- `data: ImageRawDataUpdate` - 图片数据更新对象

**返回值**: `Promise<ImageRawDataUpdateResult>` - 更新结果

**示例**:
```typescript
import { ImageRawDataUpdate } from '@evenrealities/even_hub_sdk';

const raw: Uint8Array = new Uint8Array([1, 2, 3]);

const data: ImageRawDataUpdate = {
  containerID: 1,
  containerName: 'img-1',
  imageData: raw, // SDK 会自动将 Uint8Array/ArrayBuffer 转换为 number[]
};

const result = await bridge.updateImageRawData(data);
```

**注意**: `imageData` 推荐传 `number[]`。如果传入 `Uint8Array` 或 `ArrayBuffer`，SDK 在序列化时会自动转换为 `number[]`。也可以传入 base64 字符串。

> **重要提示**：
> 1. 图片尽量选择色彩比较单一的
> 2. 图片传输禁止并发发送，必须使用队列模式发送，确保上一张发送图片的接口返回成功后再发下一张
> 3. 由于眼镜内存资源有限，尽量不要频繁发送图片

##### `textContainerUpgrade(container: TextContainerUpgrade): Promise<boolean>`

文本容器升级。

**参数**:
- `container: TextContainerUpgrade` - 文本容器升级配置

**返回值**: `Promise<boolean>` - 操作是否成功

**示例**:
```typescript
import { TextContainerUpgrade } from '@evenrealities/even_hub_sdk';

const container: TextContainerUpgrade = {
  containerID: 1,
  containerName: 'text-1', // 最大 16 个字符
  contentOffset: 0,
  contentLength: 100,
  content: '您的文本内容', // 最大 2000 个字符
};

const success = await bridge.textContainerUpgrade(container);
```

**参数要求**:
- `containerName`: 最大 16 个字符
- `content`: 最大 2000 个字符

##### `shutDownPageContainer(exitMode?: number): Promise<boolean>`

关闭页面容器。

**参数**:
- `exitMode?: number` - 退出模式（可选，默认为 0）
  - `0` = 立即退出
  - `1` = 弹出前台交互层，由用户操作决定是否退出

**返回值**: `Promise<boolean>` - 操作是否成功

**示例**:
```typescript
// 立即退出
await bridge.shutDownPageContainer(0);

// 弹出交互层
await bridge.shutDownPageContainer(1);
```

#### EvenHub 事件监听方法

##### `onEvenHubEvent(callback: (event: EvenHubEvent) => void): () => void`

监听 EvenHub 事件推送。

**参数**:
- `callback: (event: EvenHubEvent) => void` - 事件回调函数

**返回值**: `() => void` - 取消监听的函数

**示例**:
```typescript
const unsubscribe = bridge.onEvenHubEvent((event) => {
  if (event.listEvent) {
    // 处理列表事件
  } else if (event.textEvent) {
    // 处理文本事件
  } else if (event.sysEvent) {
    // 处理系统事件
  }
});

// 取消监听
unsubscribe();
```

#### 通用方法

##### `callEvenApp(method: EvenAppMethod | string, params?: any): Promise<any>`

通用方法，用于调用 Even App 的原生功能。

**参数**:
- `method: EvenAppMethod | string` - 方法名称（可使用枚举或字符串）
- `params?: any` - 方法参数（可选）

**返回值**: `Promise<any>` - Even App 方法的执行结果

**示例**:
```typescript
import { EvenAppMethod } from '@evenrealities/even_hub_sdk';

// 使用枚举
const result = await bridge.callEvenApp(EvenAppMethod.GetUserInfo);

// 使用字符串
const result = await bridge.callEvenApp('getUserInfo');
```

---

### 数据模型

#### UserInfo

用户信息模型。

**属性**:
- `uid: number` - 用户 ID
- `name: string` - 用户名
- `avatar: string` - 用户头像 URL
- `country: string` - 用户国家

**方法**:
- `toJson(): Record<string, any>` - 转换为 JSON 对象

**静态方法**:
- `fromJson(json: any): UserInfo` - 从 JSON 创建 UserInfo 实例
- `createDefault(): UserInfo` - 创建默认 UserInfo 实例

#### DeviceInfo

设备信息模型。

**属性**:
- `readonly model: DeviceModel` - 设备型号（只读）
- `readonly sn: string` - 设备序列号（只读）
- `status: DeviceStatus` - 设备状态

**方法**:
- `updateStatus(status: DeviceStatus): void` - 更新设备状态（只有当 `status.sn === device.sn` 时才会更新）
- `isGlasses(): boolean` - 检查是否为眼镜设备
- `isRing(): boolean` - 检查是否为戒指设备
- `toJson(): Record<string, any>` - 转换为 JSON 对象

**静态方法**:
- `fromJson(json: any): DeviceInfo` - 从 JSON 创建 DeviceInfo 实例

**注意**: `model` 和 `sn` 一旦创建就不可修改；只能更新 `status`。

#### DeviceStatus

设备状态模型。

**属性**:
- `readonly sn: string` - 设备序列号（只读）
- `connectType: DeviceConnectType` - 连接状态
- `isWearing?: boolean` - 是否佩戴中
- `batteryLevel?: number` - 电池电量（0-100）
- `isCharging?: boolean` - 是否正在充电
- `isInCase?: boolean` - 是否在充电盒中

**方法**:
- `toJson(): Record<string, any>` - 转换为 JSON 对象
- `isNone(): boolean` - 检查状态是否未初始化
- `isConnected(): boolean` - 检查设备是否已连接
- `isConnecting(): boolean` - 检查设备是否正在连接
- `isDisconnected(): boolean` - 检查设备是否已断开
- `isConnectionFailed(): boolean` - 检查连接是否失败

**静态方法**:
- `fromJson(json: any): DeviceStatus` - 从 JSON 创建 DeviceStatus 实例
- `createDefault(sn?: string): DeviceStatus` - 创建默认 DeviceStatus 实例

#### EvenHubEvent

EvenHub 事件模型。

**属性**:
- `listEvent?: List_ItemEvent` - 列表事件（如果存在）
- `textEvent?: Text_ItemEvent` - 文本事件（如果存在）
- `sysEvent?: Sys_ItemEvent` - 系统事件（如果存在）
- `jsonData?: Record<string, any>` - 原始 JSON 数据（可选，便于调试/回放）

**使用方式**:
开发者只需要判断哪个属性不为空，就可以直接使用对应的事件对象：

```typescript
if (event.listEvent) {
  // 处理 listEvent
} else if (event.textEvent) {
  // 处理 textEvent
} else if (event.sysEvent) {
  // 处理 sysEvent
}
```

#### 容器属性模型

这些模型定义了 EvenHub 中使用的不同容器类型的属性。

##### ListContainerProperty

列表容器配置。

**属性**:
- `xPosition?: number` - X 坐标位置（范围：0-576）
- `yPosition?: number` - Y 坐标位置（范围：0-288）
- `width?: number` - 宽度（范围：0-576）
- `height?: number` - 高度（范围：0-288）
- `borderWidth?: number` - 边框宽度（范围：0-5）
- `borderColor?: number` - 边框颜色（范围：0-15）
- `borderRdaius?: number` - 边框圆角（范围：0-10）
- `paddingLength?: number` - 内边距长度（范围：0-32）
- `containerID?: number` - 容器 ID（随机）
- `containerName?: string` - 容器名称（最大 16 个字符）
- `itemContainer?: ListItemContainerProperty` - 列表项容器配置
- `isEventCapture?: number` - 事件捕获标志（0 或 1）

##### ListItemContainerProperty

列表项容器配置。

**属性**:
- `itemCount?: number` - 列表项数量（范围：1-20）
- `itemWidth?: number` - 列表项宽度（0 = 自动填充长度，其他值 = 用户设置的固定长度）
- `isItemSelectBorderEn?: number` - 列表项选中边框启用（1 = 选中时显示外边框，0 = 隐藏）
- `itemName?: string[]` - 列表项名称数组（最多 20 项，每项最大 64 个字符）

##### TextContainerProperty

文本容器配置。

**属性**:
- `xPosition?: number` - X 坐标位置（范围：0-576）
- `yPosition?: number` - Y 坐标位置（范围：0-288）
- `width?: number` - 宽度（范围：0-576）
- `height?: number` - 高度（范围：0-288）
- `borderWidth?: number` - 边框宽度（范围：0-5）
- `borderColor?: number` - 边框颜色（范围：0-16）
- `borderRdaius?: number` - 边框圆角（范围：0-10）
- `paddingLength?: number` - 内边距长度（范围：0-32）
- `containerID?: number` - 容器 ID（随机）
- `containerName?: string` - 容器名称（最大 16 个字符）
- `isEventCapture?: number` - 事件捕获标志（0 或 1）。在一个页面上，只有最后一个容器能够处理事件。
- `content?: string` - 文本内容（最大 1000 个字符）。在启动阶段，尽量减小数据长度以满足传输效率要求。

##### TextContainerUpgrade

文本容器升级配置。

**属性**:
- `containerID?: number` - 容器 ID（随机）
- `containerName?: string` - 容器名称（最大 16 个字符）
- `contentOffset?: number` - 内容偏移量
- `contentLength?: number` - 内容长度
- `content?: string` - 文本内容（最大 2000 个字符）

##### ImageContainerProperty

图片容器配置。

**属性**:
- `xPosition?: number` - X 坐标位置（范围：0-576）
- `yPosition?: number` - Y 坐标位置（范围：0-288）
- `width?: number` - 宽度（范围：20-200）
- `height?: number` - 高度（范围：20-100）
- `containerID?: number` - 容器 ID（随机）
- `containerName?: string` - 容器名称（最大 16 个字符）

**注意**: 图片包数据量过大。在启动阶段无法传输图片内容。

##### ImageRawDataUpdate

图片原始数据更新模型。

**属性**:
- `containerID?: number` - 容器 ID
- `containerName?: string` - 容器名称
- `imageData?: number[] | string | Uint8Array | ArrayBuffer` - 图片数据（推荐：`number[]`，也可以是 base64 字符串、Uint8Array 或 ArrayBuffer）

**方法**:
- `toJson(): Record<string, any>` - 转换为 JSON 对象

**静态方法**:
- `fromJson(json: any): ImageRawDataUpdate` - 从 JSON 创建 ImageRawDataUpdate 实例

**注意**: `imageData` 推荐传 `number[]`。如果传入 `Uint8Array` 或 `ArrayBuffer`，SDK 在序列化时会自动转换为 `number[]`。也可以传入 base64 字符串。

##### CreateStartUpPageContainer

启动页容器创建模型。

**属性**:
- `containerTotalNum?: number` - 容器总数
- `listObject?: ListContainerProperty[]` - 列表容器数组
- `textObject?: TextContainerProperty[]` - 文本容器数组
- `imageObject?: ImageContainerProperty[]` - 图片容器数组

**方法**:
- `toJson(): Record<string, any>` - 转换为 JSON 对象

**静态方法**:
- `fromJson(json: any): CreateStartUpPageContainer` - 从 JSON 创建 CreateStartUpPageContainer 实例
- `toJson(model?: CreateStartUpPageContainer | Record<string, any>): Record<string, any>` - 转换为 JSON

##### RebuildPageContainer

页面容器重建模型（与 `CreateStartUpPageContainer` 结构相同）。

**属性**:
- `containerTotalNum?: number` - 容器总数
- `listObject?: ListContainerProperty[]` - 列表容器数组
- `textObject?: TextContainerProperty[]` - 文本容器数组
- `imageObject?: ImageContainerProperty[]` - 图片容器数组

**方法**:
- `toJson(): Record<string, any>` - 转换为 JSON 对象

**静态方法**:
- `fromJson(json: any): RebuildPageContainer` - 从 JSON 创建 RebuildPageContainer 实例
- `toJson(model?: RebuildPageContainer | Record<string, any>): Record<string, any>` - 转换为 JSON

**注意**: 使用 `rebuildPageContainer` 来重建页面，即使是第一个页面也要使用此方法。初始创建后不要再使用 `createStartUpPageContainer`。

### 枚举类型

#### EvenAppMethod

Even App 方法枚举。

```typescript
enum EvenAppMethod {
  GetUserInfo = 'getUserInfo',
  GetGlassesInfo = 'getGlassesInfo',
  SetLocalStorage = 'setLocalStorage',
  GetLocalStorage = 'getLocalStorage',
  CreateStartUpPageContainer = 'createStartUpPageContainer',
  RebuildPageContainer = 'rebuildPageContainer',
  UpdateImageRawData = 'updateImageRawData',
  TextContainerUpgrade = 'textContainerUpgrade',
  ShutDownPageContainer = 'shutDownPageContainer',
}
```

#### DeviceConnectType

设备连接状态枚举。

```typescript
enum DeviceConnectType {
  None = 'none',
  Connecting = 'connecting',
  Connected = 'connected',
  Disconnected = 'disconnected',
  ConnectionFailed = 'connectionFailed',
}
```

#### StartUpPageCreateResult

启动页创建结果枚举。

```typescript
enum StartUpPageCreateResult {
  Success = 0,
  Invalid = 1,
  Oversize = 2,
  OutOfMemory = 3,
}
```

## 🔧 高级用法

### 更新设备状态

`DeviceInfo` 的 `model` 和 `sn` 一旦创建就不可修改；只能更新 `status`。

```typescript
const device = await bridge.getDeviceInfo();
const status = await getDeviceStatus(); // 从其他地方获取状态

if (device && status) {
  // 只有当 status.sn === device.sn 才会更新
  device.updateStatus(status);
}
```

### 接收 App 推送的事件

App 可以通过 JS bridge 推送通知到 Web。SDK 会优先使用 `window._listenEvenAppMessage(...)` 接收。

#### 设备状态变化

App 可以推送设备状态变化，消息格式如下：

```javascript
{
  type: 'listen_even_app_data',
  method: 'deviceStatusChanged',
  data: {
    sn: 'DEVICE_SN',
    connectType: 'connected',
    isWearing: true,
    batteryLevel: 80,
    isCharging: false,
    isInCase: false
  }
}
```

#### EvenHub 事件

App 可以推送 EvenHub 事件，消息格式如下：

```javascript
{
  type: 'listen_even_app_data',
  method: 'evenHubEvent',
  data: {
    type: 'listEvent', // or textEvent/sysEvent
    jsonData: {
      containerID: 1,
      currentSelectItemName: 'item1',
      // ... 其他字段
    }
  }
}
```

SDK 兼容以下数据形态：
- `data: { type: 'listEvent', jsonData: {...} }`
- `data: { type: 'list_event', data: {...} }`
- `data: [ 'list_event', {...} ]`

### EvenHub OS Event Models

SDK 内置了 OS→App 事件的基础模型：

- `List_ItemEvent` - 列表项事件
- `Text_ItemEvent` - 文本项事件
- `Sys_ItemEvent` - 系统项事件
- `OsEventTypeList` - OS 事件类型列表（用于系统事件类型枚举）

这些模型会被 `evenHubEvent` 使用：
- `listEvent` → `List_ItemEvent`
- `textEvent` → `Text_ItemEvent`
- `sysEvent` → `Sys_ItemEvent`

#### List_ItemEvent

列表项事件模型。

**属性**:
- `containerID?: number` - 容器 ID
- `containerName?: string` - 容器名称
- `currentSelectItemName?: string` - 当前选中的项名称
- `currentSelectItemIndex?: number` - 当前选中的项索引
- `eventType?: OsEventTypeList` - 事件类型

#### Text_ItemEvent

文本项事件模型。

**属性**:
- `containerID?: number` - 容器 ID
- `containerName?: string` - 容器名称
- `eventType?: OsEventTypeList` - 事件类型

#### Sys_ItemEvent

系统项事件模型。

**属性**:
- `eventType?: OsEventTypeList` - 事件类型

**注意**: 目前 SDK 支持的事件类型为 `listEvent`、`textEvent` 和 `sysEvent`。图片事件（`imgEvent`）在协议中已定义，但当前版本的类型定义中暂未包含。

## 📝 注意事项

1. **桥接初始化**: SDK 会自动初始化桥接，但建议使用 `waitForEvenAppBridge()` 确保桥接已就绪。

2. **类型安全**: 所有接口都提供完整的 TypeScript 类型定义，建议在 TypeScript 项目中使用以获得最佳开发体验。

3. **事件监听**: 记得在组件卸载时取消事件监听，避免内存泄漏。

4. **数据格式**: EvenHub API 使用 camelCase 命名，同时兼容不同的命名约定（例如 `List_Object`）。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

- **作者**: Whiskee
- **邮箱**: whiskee.chen@evenrealities.com

## 📜 更新日志

### 0.0.1

- 初始版本发布
- 实现基础桥接功能
- 支持设备信息、用户信息、本地存储
- 支持 EvenHub 协议核心接口
- 支持事件监听机制

## 📄 许可证

本项目采用 [MIT License](https://opensource.org/licenses/MIT) 开源协议。

---

**Made with ❤️ by Even Realities**
