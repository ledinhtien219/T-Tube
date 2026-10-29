param(
  [Parameter(Mandatory=$true)][string]$InputAab,
  [Parameter(Mandatory=$true)][string]$Keystore,
  [Parameter(Mandatory=$true)][string]$Alias,
  [string]$OutputAab = ""
)
$ErrorActionPreference = "Stop"
if (!(Test-Path -LiteralPath $InputAab -PathType Leaf)) { throw "AAB not found: $InputAab" }
if (!(Test-Path -LiteralPath $Keystore -PathType Leaf)) { throw "Keystore not found: $Keystore" }
if (!(Get-Command keytool -ErrorAction SilentlyContinue)) { throw "Install Java JDK 17+ and add keytool to PATH" }
if (!(Get-Command jarsigner -ErrorAction SilentlyContinue)) { throw "Install Java JDK 17+ and add jarsigner to PATH" }
$InputAab = (Resolve-Path -LiteralPath $InputAab).Path
$Keystore = (Resolve-Path -LiteralPath $Keystore).Path
if (!$OutputAab) {
  $OutputAab = Join-Path (Split-Path -Parent $InputAab) (([IO.Path]::GetFileNameWithoutExtension($InputAab)) + "-signed.aab")
}
if (Test-Path -LiteralPath $OutputAab) { throw "Refusing to overwrite: $OutputAab" }
Write-Host "Showing upload certificate details. The tool will request the keystore password:"
& keytool -list -v -keystore $Keystore -alias $Alias
if ($LASTEXITCODE -ne 0) { throw "Unable to inspect keystore or alias" }
Write-Host ""
Write-Host "Compare the SHA256 under Certificate fingerprints above with Play Console > Play app signing > Upload key certificate."
Write-Host "Do not confuse this with the app signing certificate or SHA-256 of the AAB file."
$confirm = Read-Host "Type MATCH if the SHA256 matches the Play Console upload certificate"
if ($confirm -cne "MATCH") { throw "Cancelled: certificate match not confirmed" }
Write-Host "Signing AAB; jarsigner may request passwords securely in the terminal:"
& jarsigner -keystore $Keystore -signedjar $OutputAab $InputAab $Alias
if ($LASTEXITCODE -ne 0) { throw "jarsigner failed" }
$verifyResult = (& jarsigner -verify -verbose -certs $OutputAab 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $verifyResult -notmatch "jar verified\.") { throw "Signed AAB verification failed: $verifyResult" }
Write-Host "OK - signed and verified: $OutputAab"
Write-Host "SHA256 (file checksum):"
(Get-FileHash -LiteralPath $OutputAab -Algorithm SHA256).Hash
