$jFile = "C:\tools\fixloginjob.ps1"
$shell = "powershell.exe"
$taskName = "fix-autologin"
$jPath = "$env:HOMEDRIVE\Users\jenkins"

Out-File -InputObject (
    '$taskName = "fix-autologin"' + "`r`n" +
    '$jPath = "$env:HOMEDRIVE\Users\jenkins"' + "`r`n" +
    'if((Test-Path -path $jPath) -ne $true){' + "`r`n" +
    '$jenkinsUser = "bit-erp\jenkins"' + "`r`n" + 
    '$password = "^2kkR92mS)RS"' + "`r`n" + 
    '$logon_path = "Registry::HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon"' + "`r`n" + 
    'Set-ItemProperty -Path $logon_path -Name "AutoAdminLogon" -Value 1' + "`r`n" +
    'Set-ItemProperty -Path $logon_path -Name "DefaultUserName" -Value "$jenkinsUser"'+ "`r`n" +
    'Set-ItemProperty -Path $logon_path -Name "DefaultPassword" -Value $password'+ "`r`n" +
    'Set-ItemProperty -Path $logon_path -Name "ForceAutoLogon" -Value 1'+ "`r`n" +
    'Start-Sleep -Seconds 30'+ "`r`n" +
    'shutdown /r /t 1}'+ "`r`n" +
     'else {'+ "`r`n" +
        'Unregister-ScheduledTask -TaskName $taskName -Confirm:$False'+ "`r`n" +
    '}'
) -FilePath $jFile -Force

$Trigger= New-ScheduledTaskTrigger -AtStartup 
$Action= New-ScheduledTaskAction -Execute $shell -Argument "-File $jFile"
Register-ScheduledTask -TaskName $taskName -Trigger $Trigger -Action $Action -RunLevel "Highest" -User "SYSTEM"
Start-Sleep -Seconds 30
shutdown /r /t 1

