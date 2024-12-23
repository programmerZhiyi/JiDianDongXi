# 记点东西

一个简洁的备忘录应用，支持文字和图片混排。

## 功能特点

- 简洁的界面设计
- 支持文字和图片混合编辑
- 支持撤销操作
- 自动保存
- 图片本地存储
- 支持空标题

## 技术实现

- 使用 Room 数据库存储笔记数据
- 使用 Glide 加载图片
- 使用 Material Design 组件
- 使用 Kotlin 协程处理异步操作
- 使用 ViewModel 和 LiveData 管理数据
- 图片存储在应用私有目录

## 界面说明

### 主界面
- 顶部显示"记点东西"标题
- 右上角有新建(+)和撤回按钮
- 列表显示所有备忘录

### 编辑界面
- 支持标题和内容编辑
- 右上角可插入图片
- 图片支持删除操作
- 返回时自动保存

## 数据存储

- 使用 Room 数据库存储文本内容
- 图片以文件形式存储在应用私有目录
- 在数据库中存储图片路径
- 自动清理未使用的图片文件

## 开发环境

- Android Studio
- Kotlin
- minSdkVersion: 24
- targetSdkVersion: 33

## 依赖库

- androidx.room:room-runtime
- androidx.room:room-ktx
- com.github.bumptech.glide:glide
- com.google.android.material:material
- androidx.lifecycle:lifecycle-viewmodel-ktx
- androidx.lifecycle:lifecycle-runtime-ktx