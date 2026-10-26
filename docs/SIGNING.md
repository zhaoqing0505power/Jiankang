# Jiankang 固定签名、升级和数据迁移

Android 覆盖安装同时要求应用包名一致、签名证书匹配、versionCode 不下降。原来 GitHub Actions 的调试 APK 使用临时 Runner 签名，因此跨版本证书不同，导致覆盖失败。无法通过改版本号、关闭手机校验等绕过。

## 第一次从旧版升级

1. **不要先卸载旧版。** 在旧版“我的 → 数据备份与导出”中导出完整 JSON 文件，并确认文件已安全保存。
2. 之前安装的旧临时 debug APK 如无原来的私钥，不能直接被新签名包覆盖。导出备份后再卸载旧版。
3. 安装本次全新固定签名 release APK，打开“我的 → 数据备份与导出 → 从 JSON 备份恢复”，还原个人健康记录。
4. 今后始终安装同一固定私钥签名的 release APK，随版本号增加即可覆盖升级，正常保留原有数据。

本项目在公开仓库中保存**证书 SHA-256 指纹**而非私钥，见 ci/stable-signing-certificate.sha256。Release 构建用 Android 官方 apksigner 核实证书，指纹不符则终止发布。

## GitHub Actions 一次性配置四个 Repository secrets

打开仓库 Settings → Secrets and variables → Actions → Repository secrets ，新增：

- JIANKANG_SIGNING_KEYSTORE_B64：私人签名包内 keystore_base64.txt 全部内容
- JIANKANG_SIGNING_STORE_PASSWORD：私人签名包说明内的密钥库密码
- JIANKANG_SIGNING_KEY_PASSWORD：私人签名包说明内的私钥密码
- JIANKANG_SIGNING_KEY_ALIAS：私人签名包说明内的别名

Windows PowerShell 可从 jks 文件直接复制其 Base64 编码：

[Convert]::ToBase64String([IO.File]::ReadAllBytes("Jiankang_Release_2026.jks")) | Set-Clipboard

四项配置成功后，打开 Actions → Android build and tests → Run workflow。成功后下载名为 **Jiankang-signed-release-apk** 的构建产物。

**不要将私人签名文件/密码提交公开仓库，也不要继续把 Jiankang-debug-TEST-ONLY 用作日常升级 APK。** 私钥需由本人保存在加密的长期备份中；如果遗失，未来可能无法原位更新。

如果 GitHub Secrets 未配置，CI 只生成测试用 Debug APK，不宣称能够覆盖安装。
