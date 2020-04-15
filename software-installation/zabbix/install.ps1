If (-NOT ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator"))

{   
$arguments = "& '" + $myinvocation.mycommand.definition + "'"
Start-Process powershell -Verb runAs -ArgumentList $arguments
Break
}


New-Item -Path 'c:\Tools\Zabbix\' -ItemType Directory
Copy-Item \\172.16.50.38\share\Distr\Zabbix\zabbix_agents-4.0.0-win-amd64\bin\win64\zabbix_agentd.exe c:\Tools\Zabbix\zabbix_agentd.exe
Copy-Item \\172.16.50.38\share\Distr\Zabbix\zabbix_agentd.win.conf c:\Tools\Zabbix\zabbix_agentd.win.conf
C:\Tools\Zabbix\zabbix_agentd.exe -i -c C:\Tools\Zabbix\zabbix_agentd.win.conf
Start-Service "Zabbix Agent"
