param(
    [Parameter(Mandatory = $true)][string]$Bbox,
    [string]$Out = ".\data\osm-stations.json",
    [string]$Endpoint = "https://overpass-api.de/api/interpreter"
)
$ErrorActionPreference = "Stop"
$q = '[out:json][timeout:120];(node["amenity"="charging_station"](' + $Bbox + ');way["amenity"="charging_station"](' + $Bbox + ');relation["amenity"="charging_station"](' + $Bbox + '););out center tags;'
$body = "data=" + [System.Uri]::EscapeDataString($q)
$headers = @{ "User-Agent" = "ChargeMapPH/1.0 (contact: dev@chargemap.ph)" }
$dir = Split-Path -Parent $Out
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
# Capture the RAW response text (do not round-trip through ConvertTo-Json).
$resp = Invoke-WebRequest -Uri $Endpoint -Method Post -Body $body -ContentType "application/x-www-form-urlencoded" -Headers $headers -TimeoutSec 180 -UseBasicParsing
[System.IO.File]::WriteAllText((Resolve-Path -LiteralPath $dir).Path + "\" + (Split-Path -Leaf $Out), $resp.Content, [System.Text.Encoding]::UTF8)
$parsed = $resp.Content | ConvertFrom-Json
Write-Host ("Saved " + $parsed.elements.Count + " elements to " + $Out)
