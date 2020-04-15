$user = "bit-erp\jenkins"
$password = "^2kkR92mS)RS"
$source = "\\share\soft\scripts\jenkins-install"
$connectorName = "jenkins-start.ps1"
$dest = "$drive`:\jenkinsConnect"
$jenkinsWorkDirectory = "$drive`:\jenkins"
$taskName = "jenkins-start"
$shell = "powershell.exe"
$logon_path = 'Registry::HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon'
Set-ItemProperty -Path $logon_path -Name "AutoAdminLogon" -Value 1
Set-ItemProperty -Path $logon_path -Name "DefaultUserName" -Value "$user"
Set-ItemProperty -Path $logon_path -Name "DefaultPassword" -Value $password
Set-ItemProperty -Path $logon_path -Name "ForceAutoLogon" -Value 1


$drive = Get-PSDrive|Where-Object {$_.Free -gt 0}|Where-Object {($_.Name -eq "D") -or ($_.Name -eq "E")}

if((Test-Path -path $dest) -ne $true)
{
 New-Item -Path $dest -ItemType "directory"
 $rule=new-object System.Security.AccessControl.FileSystemAccessRule $user,"FullControl","ContainerInherit,ObjectInherit", "None","allow"
 $acl.AddAccessRule($rule)
 Set-Acl -Path $dest -AclObject $acl
}
Copy-Item $source\* -Destination $dest

if((Test-Path -path $jenkinsWorkDirectory) -ne $true)
{
 New-Item -Path $jenkinsWorkDirectory -ItemType "directory"
 $rule=new-object System.Security.AccessControl.FileSystemAccessRule $user,"FullControl","ContainerInherit,ObjectInherit", "None","allow"
 $acl.AddAccessRule($rule)
 Set-Acl -Path $jenkinsWorkDirectory -AclObject $acl
}

$Trigger= New-ScheduledTaskTrigger -AtLogOn -User $User
$Action= New-ScheduledTaskAction -Execute $shell -Argument "-File $dest\$connectorName" -WorkingDirectory $dest
Register-ScheduledTask -TaskName $taskName -Trigger $Trigger -User $User -Action $Action