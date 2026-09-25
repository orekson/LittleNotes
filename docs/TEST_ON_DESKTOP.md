# 在桌面測試小小筆記

先確定 .tools 內已有 Android SDK、JDK 與 Gradle。於專案資料夾執行：

    .\test-on-emulator.ps1

預設會建置並安裝 Personal 版，然後在 Android 模擬器開啟 App。Play 版可用：

    .\test-on-emulator.ps1 -Flavor play

模擬器視窗可直接用滑鼠與鍵盤操作；第一次啟動需等待 Android 開機完成。Personal 和 Play 使用不同 application ID，可同時安裝。