# 记点东西 (JiDianDongXi)

一个简洁的本地备忘录应用，使用 Kotlin 开发，采用 Room 数据库存储数据。

## 功能
- 创建备忘录
- 查看备忘录列表
- 查看备忘录详情
- 删除备忘录

## 技术栈
- Kotlin
- Room Database
- RecyclerView
- ViewBinding

## 项目结构
app/src/main/java/com/example/memoapp/
├── MainActivity.kt # 主界面
├── MemoDetailActivity.kt # 详情界面
├── adapter/
│ └── MemoAdapter.kt # 列表适配器
└── data/
├── Memo.kt # 数据实体
├── MemoDao.kt # 数据访问接口
└── MemoDatabase.kt # Room 数据库

## 运行要求
- Android Studio
- minSdk: 24
- targetSdk: 34

## 构建步骤
1. 克隆项目
2. 在 Android Studio 中打开
3. 运行应用