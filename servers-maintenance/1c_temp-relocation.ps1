# comments will be added soon

$user = New-Object System.Security.Principal.NTAccount("$env:computername", "USR1CV8")
$SID = $user.translate([System.Security.Principal.SecurityIdentifier])

Set-ItemProperty "Registry::\HKEY_USERS\$SID\Environment" -name TEMP -value F:\USR1CV8\temp
Set-ItemProperty "Registry::\HKEY_USERS\$SID\Environment" -name TMP -value F:\USR1CV8\temp
