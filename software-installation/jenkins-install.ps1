$user = "bit-erp\jenkins"
$password = "^2kkR92mS)RS"
$source = "\\share\soft\scripts\jenkins-install"
$connectorName = "jenkins-start-interactive.ps1"
Get-ChildItem $source
$taskName = "jenkins-start"
$shell = "powershell.exe"
$logon_path = 'Registry::HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon'
Set-ItemProperty -Path $logon_path -Name "AutoAdminLogon" -Value 1
Set-ItemProperty -Path $logon_path -Name "DefaultUserName" -Value "$user"
Set-ItemProperty -Path $logon_path -Name "DefaultPassword" -Value $password
Set-ItemProperty -Path $logon_path -Name "ForceAutoLogon" -Value 1


$jenkinsConnnectDirectory = "C:\jenkins\interactive\connect"
$jenkinsWorkDirectory = "C:\jenkins\interactive\data"

if((Test-Path -path $jenkinsConnnectDirectory) -ne $true)
{
 New-Item -Path $jenkinsConnnectDirectory -ItemType "directory"
 $acl = Get-Acl -Path $jenkinsConnnectDirectory
 $rule=new-object System.Security.AccessControl.FileSystemAccessRule $user,"FullControl","ContainerInherit,ObjectInherit", "None","allow"
 $acl.AddAccessRule($rule)
 Set-Acl -Path $jenkinsConnnectDirectory -AclObject $acl
}

if((Test-Path -path $jenkinsWorkDirectory) -ne $true)
{
 New-Item -Path $jenkinsWorkDirectory -ItemType "directory"
 $acl = Get-Acl -Path $jenkinsWorkDirectory
 $rule=new-object System.Security.AccessControl.FileSystemAccessRule $user,"FullControl","ContainerInherit,ObjectInherit", "None","allow"
 $acl.AddAccessRule($rule)
 Set-Acl -Path $jenkinsWorkDirectory -AclObject $acl
}

$startFile = "$jenkinsConnnectDirectory\jenkins-start-interactive.ps1"
Out-File -InputObject ('$label = $env:COMPUTERNAME.ToLower()' + "`r`n" + 
'$fsroot = "C:\jenkins\interactive\connect"' + "`r`n" + 
'$args = "-jar swarm-client-3.9.jar -master http://172.16.10.14:8080/ -username jenkins -password ""^2kkR92mS)RS"" -executors 3 -labels $label -fsroot $fsroot"' + "`r`n" + 
'Start-Process java -ArgumentList $args') -FilePath $startFile
Copy-Item "$source\swarm-client-3.9.jar" -Destination $jenkinsConnnectDirectory


$Trigger= New-ScheduledTaskTrigger -AtLogOn -User $User
$Action= New-ScheduledTaskAction -Execute $shell -Argument "-File $jenkinsConnnectDirectory\$connectorName" -WorkingDirectory $jenkinsConnnectDirectory
Register-ScheduledTask -TaskName $taskName -Trigger $Trigger -User $User -Action $Action