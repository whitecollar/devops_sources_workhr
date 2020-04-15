
$source = "\\share\soft\scripts\restart1c.ps1"
$dest = "C:\Tools"
if((Test-Path -path $dest) -ne $true)
{
 New-Item -Path $dest -ItemType "directory"
}
Start-Sleep -Seconds 1
Copy-Item $source -Destination $dest
$Trigger= New-ScheduledTaskTrigger -Daily -At 23:00 
$Action= New-ScheduledTaskAction  -Execute "powershell.exe" -Argument '-File "C:\tools\restart1c.ps1"'
Register-ScheduledTask -TaskName "restart 1c" -Trigger $Trigger -User "NT AUTHORITY\SYSTEM" -Action $Action -RunLevel Highest