$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$projectRoot = [System.IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$sourceRoot = Join-Path $projectRoot 'src\finalPPKG\ui\components\icons'
$resourceRoot = Join-Path $projectRoot 'resources\icons'
$sourcePrefix = $sourceRoot.TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar

# Java's standard ImageIO reads PNG, so keep portable runtime copies of the supplied ICO artwork.
foreach ($file in Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter '*.ico') {
    $relativePath = [System.IO.Path]::ChangeExtension($file.FullName.Substring($sourcePrefix.Length), '.png')
    $destination = Join-Path $resourceRoot $relativePath
    New-Item -ItemType Directory -Path ([System.IO.Path]::GetDirectoryName($destination)) -Force | Out-Null
    $icon = New-Object System.Drawing.Icon($file.FullName)
    $bitmap = $null
    try {
        $bitmap = $icon.ToBitmap()
        $bitmap.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        if ($null -ne $bitmap) { $bitmap.Dispose() }
        $icon.Dispose()
    }
}
Write-Host 'Updated the PNG runtime icons in resources/icons.'
