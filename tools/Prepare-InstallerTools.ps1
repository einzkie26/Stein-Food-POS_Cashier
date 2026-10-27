$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$projectRoot = [System.IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$config = Get-Content -LiteralPath (Join-Path $projectRoot 'installer\app.json') -Raw | ConvertFrom-Json
$toolchainPath = Join-Path $projectRoot 'build\toolchain'
$wixDirectory = Join-Path $toolchainPath 'wix'
$wixExecutable = Join-Path $wixDirectory 'wix.exe'
$savedEnvironment = @{}
$settings = @{
    DOTNET_CLI_HOME = (Join-Path $toolchainPath 'dotnet-home')
    NUGET_PACKAGES = (Join-Path $toolchainPath 'nuget')
    DOTNET_ADD_GLOBAL_TOOLS_TO_PATH = 'false'
    DOTNET_CLI_TELEMETRY_OPTOUT = '1'
    DOTNET_NOLOGO = '1'
    WIX_EXTENSIONS = $toolchainPath
}

try {
    foreach ($setting in $settings.GetEnumerator()) {
        $savedEnvironment[$setting.Key] = [Environment]::GetEnvironmentVariable($setting.Key, 'Process')
        [Environment]::SetEnvironmentVariable($setting.Key, $setting.Value, 'Process')
    }
    if (-not (Test-Path -LiteralPath $wixExecutable)) {
        if (-not (Get-Command dotnet -ErrorAction SilentlyContinue)) {
            throw 'Install the .NET 8 SDK to prepare the local installer tools: https://dotnet.microsoft.com/download/dotnet/8.0'
        }
        Write-Host "Preparing project-local WiX $($config.wixVersion) (first build needs internet access)..."
        & dotnet tool install wix --version $config.wixVersion --tool-path $wixDirectory --add-source https://api.nuget.org/v3/index.json | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'Could not download the local WiX tool. Check internet access and the .NET 8 SDK.' }
    }
    $installedVersion = (& $wixExecutable --version | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or -not $installedVersion.StartsWith($config.wixVersion + '+')) {
        throw "Expected WiX $($config.wixVersion) at $wixExecutable; found $installedVersion."
    }
    foreach ($extension in @('WixToolset.UI.wixext', 'WixToolset.Util.wixext')) {
        $assembly = Join-Path $toolchainPath ".wix\extensions\$extension\$($config.wixVersion)\wixext5\$extension.dll"
        if (-not (Test-Path -LiteralPath $assembly)) {
            # WIX_EXTENSIONS redirects this user-scope cache entirely into build/toolchain.
            & $wixExecutable extension add --global "$extension/$($config.wixVersion)" | Out-Host
            if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $assembly)) {
                throw "Could not prepare $extension. Check internet access and retry."
            }
        }
    }
    [PSCustomObject]@{ WixDirectory = $wixDirectory; ExtensionsHome = $toolchainPath; Version = $installedVersion }
}
finally {
    foreach ($setting in $savedEnvironment.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($setting.Key, $setting.Value, 'Process')
    }
}
