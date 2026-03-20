param(
    [Parameter(Mandatory = $true)]
    [string]$Key,

    [Parameter(Mandatory = $true)]
    [string]$Value
)

$sha = [System.Security.Cryptography.SHA256]::Create()
$hash = $sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($Key))

$aes = [System.Security.Cryptography.Aes]::Create()
$aes.Mode = [System.Security.Cryptography.CipherMode]::CBC
$aes.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
$aes.KeySize = 128
$aes.BlockSize = 128
$aes.Key = $hash[0..15]
$aes.GenerateIV()

$encryptor = $aes.CreateEncryptor()
$plainBytes = [System.Text.Encoding]::UTF8.GetBytes($Value)
$cipherBytes = $encryptor.TransformFinalBlock($plainBytes, 0, $plainBytes.Length)

$combined = New-Object byte[] ($aes.IV.Length + $cipherBytes.Length)
[Array]::Copy($aes.IV, 0, $combined, 0, $aes.IV.Length)
[Array]::Copy($cipherBytes, 0, $combined, $aes.IV.Length, $cipherBytes.Length)

$result = [Convert]::ToBase64String($combined)
Write-Output ("ENC(" + $result + ")")
