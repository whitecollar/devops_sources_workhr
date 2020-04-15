$User="$env:COMPUTERNAME\jenkins_console"
$Trigger= New-ScheduledTaskTrigger -AtLogOn -User $User
$Action= New-ScheduledTaskAction -Execute "D:\jenkins\start.cmd" -WorkingDirectory "D:\jenkins"
Register-ScheduledTask -TaskName "jenkins-start" -Trigger $Trigger -User $User -Action $Action 