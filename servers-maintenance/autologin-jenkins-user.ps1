
$user = "bit-erp\jenkins"
$password = "^2kkR92mS)RS"
$logon_path = 'Registry::HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon'

Set-ItemProperty -Path $logon_path -Name "AutoAdminLogon" -Value 1
Set-ItemProperty -Path $logon_path -Name "DefaultUserName" -Value "$user"
Set-ItemProperty -Path $logon_path -Name "DefaultPassword" -Value $password
Set-ItemProperty -Path $logon_path -Name "ForceAutoLogon" -Value 1
