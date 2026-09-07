param(
    [string]$BaseUrl = "https://olifan.pixsom.fr",
    [string]$EmailSuffix = "@loadtest.invalid"
)

$adminEmail = Read-Host "Email administrateur"
$secureAccessCode = Read-Host "Mot de passe administrateur" -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureAccessCode)

try {
    $accessCode = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    $loginBody = @{ email = $adminEmail; accessCode = $accessCode } | ConvertTo-Json
    $session = Invoke-RestMethod `
        -Uri "$($BaseUrl.TrimEnd('/'))/api/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    $accessCode = $null
    $loginBody = $null
}

$headers = @{ Authorization = "Bearer $($session.token)" }
$users = Invoke-RestMethod `
    -Uri "$($BaseUrl.TrimEnd('/'))/api/admin/users" `
    -Method Get `
    -Headers $headers

$targets = @($users | Where-Object { $_.email.EndsWith($EmailSuffix, [StringComparison]::OrdinalIgnoreCase) })
if ($targets.Count -eq 0) {
    "Aucun compte se terminant par $EmailSuffix."
    exit 0
}

"$($targets.Count) compte(s) seront supprimés."
$confirmation = Read-Host "Tapez SUPPRIMER pour confirmer"
if ($confirmation -cne "SUPPRIMER") {
    "Suppression annulée."
    exit 0
}

foreach ($user in $targets) {
    Invoke-RestMethod `
        -Uri "$($BaseUrl.TrimEnd('/'))/api/admin/users/$($user.id)" `
        -Method Delete `
        -Headers $headers
}

"$($targets.Count) compte(s) de test supprimé(s)."
