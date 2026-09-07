param(
    [ValidateRange(1, 2000)]
    [int]$Count = 150,
    [string]$OutputPath = (Join-Path $PSScriptRoot "users.csv")
)

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("Email professionnel;Prénom;Nom")

for ($index = 1; $index -le $Count; $index++) {
    $number = $index.ToString("000")
    $lines.Add("loadtest$number@loadtest.invalid;Charge;Test $number")
}

[System.IO.File]::WriteAllLines(
    $OutputPath,
    $lines,
    [System.Text.UTF8Encoding]::new($false)
)

"$Count participants créés dans $OutputPath"
