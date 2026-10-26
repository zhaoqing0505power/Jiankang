# 健康日记 · Jiankang

原生 Android 健康记录应用。按照已确认的十屏原型实现：首页、今日记录、身体数据、早餐、午餐、睡眠、运动、数据统计、营养建议、我的，并包含晚餐及设置子页面。

- 每日身高、体重、血压记录；BMI（体重指数）即时计算。
- 三餐食物选择、份量、粗略热量和常用食物收藏。
- 入睡/醒来时间选择，自动处理跨日睡眠，8 小时目标。
- 运动类型、分钟、消耗热量（可按体重估算）。
- 体重、血压、睡眠和运动趋势；地中海饮食习惯提示。
- 数据只存手机本地 SQLite，支持 JSON 备份导入与 CSV 导出，无需账号和联网。

重要：食物能量、运动消耗和饮食评分均为个人习惯记录用的估算值，不用于医学诊断。首次安装数据库为空，不包含演示数据。

## 构建

使用 Android Studio 打开此仓库（JDK 17，Android SDK API 36）。支持 Gradle 8.13、Android Gradle Plugin 8.11.1、Kotlin 2.2.20。执行 gradle :app:assembleDebug，产物位于 app/build/outputs/apk/debug/app-debug.apk。

GitHub Actions 中的 Android workflow 会自动编译、运行单元测试，并将可下载的 APK 上传为构建产物。工程初版不接入健康服务：个人健康数据未被静默读取或上传。

## 功能说明

具体页面、离线存储与隐私细节见 docs/IMPLEMENTATION.md。
