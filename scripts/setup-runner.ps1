#Requires -RunAsAdministrator
# Cài GitHub self-hosted runner trên máy Windows (chạy 1 lần).
# Cách dùng: mở PowerShell bằng "Run as administrator", rồi:
#   Set-ExecutionPolicy -Scope Process Bypass
#   .\scripts\setup-runner.ps1

param(
    [string]$RepoUrl   = "https://github.com/Trungdat35/datdofishing-be",
    [string]$RunnerDir = "C:\actions-runner",
    [string]$RunnerName = "datdo-server",
    [string]$Labels    = "datdo-server"
)

$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force -Path $RunnerDir | Out-Null
Set-Location $RunnerDir

if (-not (Test-Path ".\config.cmd")) {
    Write-Host "Tải GitHub Actions runner phiên bản mới nhất..." -ForegroundColor Cyan
    $rel = Invoke-RestMethod -Uri "https://api.github.com/repos/actions/runner/releases/latest" `
           -Headers @{ "User-Agent" = "datdo-setup" }
    $asset = $rel.assets | Where-Object { $_.name -like "actions-runner-win-x64-*.zip" } | Select-Object -First 1
    if (-not $asset) { throw "Không tìm thấy file runner cho Windows x64." }
    Invoke-WebRequest -Uri $asset.browser_download_url -OutFile "runner.zip"
    Expand-Archive -Path "runner.zip" -DestinationPath $RunnerDir -Force
    Remove-Item "runner.zip"
}

Write-Host ""
Write-Host "Mở GitHub: $RepoUrl/settings/actions/runners/new (chọn Windows x64)" -ForegroundColor Yellow
Write-Host "Copy token trong phần 'Configure' (token chỉ có hiệu lực khoảng 1 giờ)." -ForegroundColor Yellow
$token = Read-Host "Dán registration token"

.\config.cmd --url $RepoUrl --token $token --name $RunnerName --labels $Labels `
    --work _work --unattended --replace
if ($LASTEXITCODE -ne 0) { throw "config.cmd thất bại, xem log phía trên." }

.\svc.cmd install
if ($LASTEXITCODE -ne 0) { throw "Cài service thất bại." }
.\svc.cmd start

Write-Host ""
Write-Host "Xong. Kiểm tra trên GitHub: Settings > Actions > Runners, runner '$RunnerName' phải là Idle." -ForegroundColor Green
Write-Host "Lưu ý: service mặc định chạy bằng tài khoản hệ thống, không gọi được Docker Desktop." -ForegroundColor Yellow
Write-Host "Nếu 'docker ps' lỗi trong job, cài lại service bằng tài khoản Windows của bạn theo tài liệu GitHub, hoặc chạy .\run.cmd trong cửa sổ luôn mở." -ForegroundColor Yellow
