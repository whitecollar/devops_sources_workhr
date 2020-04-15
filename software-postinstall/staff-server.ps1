
$net = new-object -ComObject WScript.Network
$net.MapNetworkDrive("r:", "\\share\soft", $false, "bit-erp\smb", "f3KyhEJu")
$source = "r:\"
$ciMaster = "ci-master.bit-erp.loc"
$ciMasterPort = "8080"
$ciMasterAddr = "$ciMaster`:$ciMasterPort"
$jenkinsUser = "bit-erp\jenkins"
$jenkinsPassword = "^2kkR92mS)RS"

msiexec /i $source\tightvnc-2.8.11-gpl-setup-64bit.msi /quiet /norestart ADDLOCAL="Server,Viewer" SERVER_REGISTER_AS_SERVICE=1 SERVER_ADD_FIREWALL_EXCEPTION=1 VIEWER_ADD_FIREWALL_EXCEPTION=1 SET_USECONTROLAUTHENTICATION=1 VALUE_OF_USECONTROLAUTHENTICATION=0 SET_USEVNCAUTHENTICATION=1 VALUE_OF_USEVNCAUTHENTICATION=0 SET_ALLOWLOOPBACK=1 VALUE_OF_ALLOWLOOPBACK=1

# Отключаем Конфигурацию усиленной безопасности IE
$AdminKey = "HKLM:\SOFTWARE\Microsoft\Active Setup\Installed Components\{A509B1A7-37EF-4b3f-8CFC-4F3A74704073}"
    $UserKey = "HKLM:\SOFTWARE\Microsoft\Active Setup\Installed Components\{A509B1A8-37EF-4b3f-8CFC-4F3A74704073}"
    Set-ItemProperty -Path $AdminKey -Name "IsInstalled" -Value 0
    Set-ItemProperty -Path $UserKey -Name "IsInstalled" -Value 0

# Windows Defender исключения
$paths = "C:\jenkins","$env:Homedir\Users\jenkis"
Foreach ($pathex in $paths){
Add-MpPreference -ExclusionPath $pathex
}
# .Net 4.7.1
.$source\NDP471-KB4033342-x86-x64-AllOS-ENU.exe /q /norestart|Out-Null

# RDS-Services
Install-WindowsFeature -Name Remote-Desktop-Services,RDS-Licensing,RDS-RD-Server
Install-WindowsFeature -Name RSAT-RDS-Tools -IncludeAllSubFeature

Enable-PSRemoting -Force

# Nircmd
Copy-Item $source\nircmd-x64\* -Destination $env:windir

# TightVNC
msiexec /i $source\tightvnc-2.8.11-gpl-setup-64bit.msi /quiet /norestart ADDLOCAL="Server,Viewer" SERVER_REGISTER_AS_SERVICE=1 SERVER_ADD_FIREWALL_EXCEPTION=1 VIEWER_ADD_FIREWALL_EXCEPTION=1 SET_USECONTROLAUTHENTICATION=1 VALUE_OF_USECONTROLAUTHENTICATION=0 SET_USEVNCAUTHENTICATION=1 VALUE_OF_USEVNCAUTHENTICATION=0 SET_ALLOWLOOPBACK=1 VALUE_OF_ALLOWLOOPBACK=1

# VS Code satup
.$source\VSCodeSetup-x64-1.26.0 /verysilent|Out-null
Write-Host "VS Code installed"

# VC Redist
.$source\vcredist_x86_13.exe /install /quiet|Out-Null
.$source\vcredist_x64_13.exe /install /quiet|Out-Null
Write-Host "VC redist installed"

msiexec /i $source\msodbcsql.msi /passive IACCEPTMSODBCSQLLICENSETERMS=YES |Out-Null
msiexec /i $source\MsSqlCmdLnUtils.msi  /passive IACCEPTMSSQLCMDLNUTILSLICENSETERMS=YES |Out-Null

#Git
.$source\Git-2.18.0-64-bit.exe /verysilent|Out-Null
Write-Host "Git installed"
$gitBin = "C:\Program Files\Git\bin"
.$gitBin\git.exe config --system core.longpaths true

.$source\git-lfs-windows-v2.5.2.exe /verysilent|Out-Null
Write-Host "Git lfs installed!"

# for Git credentials
Install-PackageProvider Nuget –Force
install-Module CredentialManager -force

# 1C EDT 
.$source\1c_enterprise_development_tools_distr_1.9.2_53_windows_x86_64\1ce-installer-cli.cmd install --ignore-hardware-checks|Out-null

# OneScript
.$source\OneScript-1.0.21-setup.exe /silent|Out-Null
Write-Host "OScript installed"

#Far
msiexec /i $source\Far30b5254.x64.20180805.msi /quiet|Out-Null
Write-Host "FAR installed"

#7z
.$source\7z1801-x64.exe /S|Out-Null
Write-Host "7Z installed"

#devops-221 add instal zabbix-agent ver 4.0.0
New-Item -Path 'c:\Tools\Zabbix\' -ItemType Directory
Copy-Item $source\Zabbix\zabbix_agents-4.0.0-win-amd64\bin\zabbix_agentd.exe c:\Tools\Zabbix\zabbix_agentd.exe
Copy-Item $source\Zabbix\zabbix_agentd.win.conf c:\Tools\Zabbix\zabbix_agentd.win.conf
C:\Tools\Zabbix\zabbix_agentd.exe -i -c C:\Tools\Zabbix\zabbix_agentd.win.conf
Start-Service "Zabbix Agent"

# consul
$consulDistr = "$source\consul\windows"
$consulHome = "C:\consul\"
$consulConfig = "C:\consul\etc\consul.d"
$dataDir = "C:\consul\var\consul"
$consulService = "Consul-client 1.4"
New-Item -ItemType Directory -Path $consulConfig
New-Item -ItemType Directory -Path $dataDir
Copy-Item "$consulDistr\consul.exe" -Destination $consulHome
Copy-Item "$consulDistr\client-config.json" -Destination $consulConfig

New-Service -Name $consulService -BinaryPathName "$consulHome\consul.exe agent -config-dir=$consulConfig -ui"
Start-Sleep -Seconds 5
Start-Service -Name $consulService

function RunConsole($scriptBlock)
{
    # Популярное решение "устранения" ошибки: Exception setting "OutputEncoding": "The handle is invalid."
    & cmd /c ver | Out-Null

    $encoding = [Console]::OutputEncoding 
    [Console]::OutputEncoding = [System.Text.Encoding]::GetEncoding("cp866")
    try
    {
        &$scriptBlock
    }
    finally
    {
        [Console]::OutputEncoding = $encoding
    }
}

RunConsole {
$oscriptpath = "C:\Program Files (x86)\OneScript\bin"
$opmpath = "C:\Program Files (x86)\OneScript\lib\opm\src\cmd\opm.os"
$libs = ("add,gitsync,vanessa-runner,v8runner,irac").Split(",")
$oscriptarg = '"'+"$opmpath"+'"'+" install"
foreach ($lib in $libs){
$oscriptargs = $oscriptarg+" "+$lib
Start-Process "$oscriptpath\oscript.exe" -ArgumentList $oscriptargs -Wait
}
}

# Jenkins-slave service
$jenkinsSource = "$source\scripts\jenkins-install"
$jenkinsServiceDirectory = "C:\jenkins\service\connect"
$fsroot = "C:\jenkins\service\data"
$jreVersion = (Get-ChildItem "$env:ProgramFiles\java\" -Exclude "jdk*").Name
$javaPath = "$env:ProgramFiles\java\$jreVersion\bin\java.exe"

if((Test-Path -path $jenkinsServiceDirectory) -ne $true)
{
 New-Item -Path $jenkinsServiceDirectory -ItemType "directory"
 }

Copy-Item "$jenkinsSource\nssm.exe" -Destination $jenkinsServiceDirectory
Copy-Item "$jenkinsSource\swarm-client-3.9.jar" -Destination $jenkinsServiceDirectory
$nssmPath = "$jenkinsServiceDirectory\nssm.exe"
$label = $env:COMPUTERNAME.ToLower()+"service"
$arguments = "-jar $jenkinsServiceDirectory\swarm-client-3.9.jar -master http://$ciMasterAddr/ -username jenkins -password ""$jenkinsPassword"" -executors 3 -labels $label -fsroot $fsroot"
& $nssmPath install jenkins-service $javaPath
& $nssmPath set jenkins-service AppParameters $arguments
& $nssmPath start jenkins-service

# jenkins-slave interactive


$connectorName = "jenkins-start-interactive.ps1"
$taskName = "jenkins-start"
$shell = "powershell.exe"
$logon_path = 'Registry::HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon'
Set-ItemProperty -Path $logon_path -Name "AutoAdminLogon" -Value 1
Set-ItemProperty -Path $logon_path -Name "DefaultUserName" -Value "$jenkinsUser"
Set-ItemProperty -Path $logon_path -Name "DefaultPassword" -Value $jenkinsPassword
Set-ItemProperty -Path $logon_path -Name "ForceAutoLogon" -Value 1


$jenkinsConnnectDirectory = "C:\jenkins\interactive\connect"
$jenkinsWorkDirectory = "C:\jenkins\interactive\data"

if((Test-Path -path $jenkinsConnnectDirectory) -ne $true)
{
 New-Item -Path $jenkinsConnnectDirectory -ItemType "directory"
 $acl = Get-Acl -Path $jenkinsConnnectDirectory
 $rule=new-object System.Security.AccessControl.FileSystemAccessRule $jenkinsUser,"FullControl","ContainerInherit,ObjectInherit", "None","allow"
 $acl.AddAccessRule($rule)
 Set-Acl -Path $jenkinsConnnectDirectory -AclObject $acl
}

if((Test-Path -path $jenkinsWorkDirectory) -ne $true)
{
 New-Item -Path $jenkinsWorkDirectory -ItemType "directory"
 $acl = Get-Acl -Path $jenkinsWorkDirectory
 $rule=new-object System.Security.AccessControl.FileSystemAccessRule $jenkinsUser,"FullControl","ContainerInherit,ObjectInherit", "None","allow"
 $acl.AddAccessRule($rule)
 Set-Acl -Path $jenkinsWorkDirectory -AclObject $acl
}

$startFile = "$jenkinsConnnectDirectory\jenkins-start-interactive.ps1"
Out-File -InputObject ('$label = $env:COMPUTERNAME.ToLower()' + "`r`n" + 
'$fsroot = "C:\jenkins\interactive\data"' + "`r`n" + 
'$args = "-jar swarm-client-3.9.jar -master http://ci-master.bit-erp.loc:8080/ -username jenkins -password ""^2kkR92mS)RS"" -executors 3 -labels $label -fsroot $fsroot"'+ "`r`n" + 
'$JAVA_OPTS = "-Dfile.encoding=UTF-8"'+ "`r`n" + 
'while ($true) {' + "`r`n" + 
'    Start-Process java -Wait -ArgumentList $JAVA_OPTS, $args' + "`r`n" + 
'}'
) -FilePath $startFile
Copy-Item "$jenkinsSource\swarm-client-3.9.jar" -Destination $jenkinsConnnectDirectory


$Trigger= New-ScheduledTaskTrigger -AtLogOn -User $jenkinsUser
$Action= New-ScheduledTaskAction -Execute $shell -Argument "-File $jenkinsConnnectDirectory\$connectorName" -WorkingDirectory $jenkinsConnnectDirectory
Register-ScheduledTask -TaskName $taskName -Trigger $Trigger -User $jenkinsUser -Action $Action

# fix autologin lenkins 
$jFile = "C:\tools\fixloginjob.ps1"
$shell = "powershell.exe"
$taskName = "fix-autologin"
#$jPath = "$env:HOMEDRIVE\Users\jenkins"

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
# -----------------------

# Добавляем пункт контекстного меню отрыть папку с VSCode
New-Item -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode
New-Item -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode\command
New-ItemProperty -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode -Name "(default)" -Value "Open w&ith Code"
New-ItemProperty -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode -Name "Icon" -Value "C:\Program Files\Microsoft VS Code\Code.exe"
New-ItemProperty -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode\command -Name "(default)" -Value "C:\Program Files\Microsoft VS Code\Code.exe %V"

# Ярлыки на общий рабочий стол
New-Item -ItemType SymbolicLink -Path "C:\Users\Public\Desktop" -Name "Far.lnk" -Value "C:\Program Files\Far Manager\Far.exe"

$sourceTreeExe = (Get-ChildItem $source |Where-Object {$_.Name -like "SourceTreeSetup*"}).Name
Copy-Item "$source\$sourceTreeExe" -Destination "$env:PUBLIC\Downloads"
New-Item -ItemType SymbolicLink -Path "C:\Users\Public\Desktop" -Name "SourceTreeSetup.lnk" -Value "$env:PUBLIC\Downloads\$sourceTreeExe"

# Google Chrome
$LocalTempDir = $env:TEMP;
$ChromeInstaller = "ChromeInstaller.exe";
(new-object System.Net.WebClient).DownloadFile('http://dl.google.com/chrome/install/375.126/chrome_installer.exe', "$LocalTempDir\$ChromeInstaller");
& "$LocalTempDir\$ChromeInstaller" /silent /install;
$Process2Monitor =  "ChromeInstaller";
Do {
    $ProcessesFound = Get-Process | Where-Object{$_.Name -contains $Process2Monitor} | Select-Object -ExpandProperty Name;
    If ($ProcessesFound) {
        Write-Host "Still running: Installer";
        Start-Sleep -Seconds 2 }
    else {
        rm "$LocalTempDir\$ChromeInstaller" -ErrorAction SilentlyContinue -Verbose}
    } 
Until (!$ProcessesFound)

# MS Office
.$source\Office2013x64standart\setup.exe /adminfile of2013.msp
Start-Sleep -Seconds 5
$Process2Monitor =  "setup";
Do {
    $ProcessesFound = Get-Process | Where-Object{$_.Name -contains $Process2Monitor} | Select-Object -ExpandProperty Name;
    If ($ProcessesFound) {
        Write-Host "Still running: Installer";
        Start-Sleep -Seconds 2 }
    else {
        Write-Host "Done!"}
    } 
Until (!$ProcessesFound)
 
Add-LocalGroupMember -Group "Пользователи удаленного рабочего стола" -Member "bit-erp\biterp"

#DEVOPS-558 
$jreVersion = (Get-ChildItem "$env:ProgramFiles\java\" -Exclude "jdk*").Name
$javaPath = "$env:ProgramFiles\java\$jreVersion\bin\"
$javaHome = "$env:ProgramFiles\java\$jreVersion" # DEVOPS-882
[Environment]::SetEnvironmentVariable(
    "Path",
    [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::Machine) + ";$javaPath",
    [EnvironmentVariableTarget]::Machine)
	
[Environment]::SetEnvironmentVariable(
    "JAVA_HOME",
    "$javaHome",
    [EnvironmentVariableTarget]::Machine)

shutdown /r /t 1
