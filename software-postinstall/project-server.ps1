$sapwd= $args[0]
$domain = "bit-erp.loc"
$password = "s9z8Uj123" | ConvertTo-SecureString -asPlainText -Force
$username = "$domain\devopsservices" 
$credential = New-Object System.Management.Automation.PSCredential($username,$password)

$net = new-object -ComObject WScript.Network
$net.MapNetworkDrive("r:", "\\share\soft", $false, $username, "s9z8Uj123")
$source = "r:"

$adminKey = "HKLM:\SOFTWARE\Microsoft\Active Setup\Installed Components\{A509B1A7-37EF-4b3f-8CFC-4F3A74704073}"
$userKey = "HKLM:\SOFTWARE\Microsoft\Active Setup\Installed Components\{A509B1A8-37EF-4b3f-8CFC-4F3A74704073}"
Set-ItemProperty -Path $adminKey -Name "IsInstalled" -Value 0
 Set-ItemProperty -Path $userKey -Name "IsInstalled" -Value 0


# RDS-Services
Install-WindowsFeature -Name Remote-Desktop-Services,RDS-Licensing,RDS-RD-Server
Install-WindowsFeature -Name RSAT-RDS-Tools -IncludeAllSubFeature

# IIS
Install-WindowsFeature -Name Web-Server,Web-Basic-Auth,Web-Windows-Auth,Web-Health,Web-Http-Logging,Web-Request-Monitor,Web-Log-Libraries,Web-Http-Tracing,Web-Common-Http,Web-Default-Doc,Web-Dir-Browsing,Web-Http-Errors,Web-Static-Content,Web-Http-Redirect,Web-Performance,Web-App-Dev,Web-Mgmt-Tools,Web-WebSockets,Web-ISAPI-Ext,Web-ISAPI-Filter,Web-AppInit

# .Net 4.7.1
.$source\NDP471-KB4033342-x86-x64-AllOS-ENU.exe /q /norestart|Out-Null

Add-Type -AssemblyName System.Web
$url='https://hooks.slack.com/services/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'
$serverName = $env:COMPUTERNAME
Add-Type -AssemblyName System.Net.Http
$http = New-Object -TypeName System.Net.Http.Httpclient
$message = "Server: " +$serverName + " sa password: " + $sapwd
$httpMessage = "{""text"": """ + $message + """}";
$content = New-Object -TypeName System.Net.Http.StringContent($httpMessage)
$http.PostAsync("$url", $content).ResultHost

$sw = [Diagnostics.Stopwatch]::StartNew()
$currentUserName = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name;
$sqlServerIsoImagePath = "$source\ISO\sql.iso"

#Mount the installation media, and change to the mounted Drive.
$mountVolume = Mount-DiskImage -ImagePath $sqlServerIsoImagePath -PassThru
$driveLetter = ($mountVolume | Get-Volume).DriveLetter
$drivePath = $driveLetter + ":"
push-location -path "$drivePath"

#.\Setup.exe /q /ACTION=Install /FEATURES=SQLEngine, LocalDB /UpdateEnabled /UpdateSource=MU /X86=false /INSTANCENAME=MSSQLSERVER /INSTALLSHAREDDIR="C:\Program Files\Microsoft SQL Server" /INSTALLSHAREDWOWDIR="C:\Program Files (x86)\Microsoft SQL Server" /SQLSVCINSTANTFILEINIT="True" /INSTANCEDIR="C:\Program Files\Microsoft SQL Server" /AGTSVCACCOUNT="NT Service\SQLSERVERAGENT" /AGTSVCSTARTUPTYPE="Manual" /SQLSVCSTARTUPTYPE="Automatic" /SQLCOLLATION="SQL_Latin1_General_CP1_CI_AS" /SQLSVCACCOUNT="NT Service\MSSQLSERVER" /SECURITYMODE="SQL"  /SQLSYSADMINACCOUNTS="$currentUserName" /IACCEPTSQLSERVERLICENSETERMS /SAPWD="$SecPwd"
.\Setup.exe /q /ACTION=Install /FEATURES=SQLEngine, LocalDB /TCPENABLED="1" /UpdateEnabled=false /X86=false /INSTANCENAME=MSSQLSERVER /INSTALLSHAREDDIR="C:\Program Files\Microsoft SQL Server" /INSTALLSHAREDWOWDIR="C:\Program Files (x86)\Microsoft SQL Server" /SQLSVCINSTANTFILEINIT="True" /INSTANCEDIR="C:\Program Files\Microsoft SQL Server" /SQLBACKUPDIR="D:\SQL\BACKUP" /SQLUSERDBDIR="D:\SQL\DATA" /AGTSVCACCOUNT="NT Service\SQLSERVERAGENT" /AGTSVCSTARTUPTYPE="Manual" /SQLSVCSTARTUPTYPE="Automatic" /SQLCOLLATION="Cyrillic_General_CI_AS" /SQLSVCACCOUNT="NT Service\MSSQLSERVER" /SECURITYMODE="SQL"  /SQLSYSADMINACCOUNTS="$currentUserName" /IACCEPTSQLSERVERLICENSETERMS /SAPWD="$sapwd"

#Dismount the installation media.
pop-location
Dismount-DiskImage -ImagePath $sqlServerIsoImagePath

#print Time taken to execute
$sw.Stop()
"Sql install script completed in {0:c}" -f $sw.Elapsed;


$filepath="$source\ISO\SSMS-Setup-ENU.exe"

# start the SSMS installer
Write-Verbose "Beginning SSMS 2016 install..." -Verbose
$Parms = " /Install /Quiet /Norestart /Logs log.txt"
$Prms = $Parms.Split(" ")
& "$filepath" $Prms | Out-Null
Write-Verbose "SSMS installation complete" -Verbose


# recovery Simple by default
Import-Module -Name SqlServer
Invoke-Sqlcmd -Username "sa" -Password $sapwd -Query "USE master; ALTER DATABASE model SET RECOVERY SIMPLE;"

# DEVOPS-493
Invoke-Sqlcmd -Username "sa" -Password $sapwd -Query "
sp_configure 'show advanced options', 1;
GO
RECONFIGURE;
GO
sp_configure 'max server memory', 6192;
GO
RECONFIGURE;
GO"


# VS Code satup
.$source\VSCodeSetup-x64-1.26.0 /verysilent|Out-null
Write-Verbose "VS Code installed" -Verbose

# VC Redist
.$source\vcredist_x86_13.exe /install /quiet|Out-Null
.$source\vcredist_x64_13.exe /install /quiet|Out-Null
Write-Verbose "VC redist installed" -Verbose

msiexec /i $source\msodbcsql.msi /passive IACCEPTMSODBCSQLLICENSETERMS=YES |Out-Null
msiexec /i $source\MsSqlCmdLnUtils.msi  /passive IACCEPTMSSQLCMDLNUTILSLICENSETERMS=YES |Out-Null

# Git
.$source\Git-2.18.0-64-bit.exe /verysilent|Out-Null
.$source\git-lfs-windows-v2.5.2.exe /verysilent|Out-Null
$gitBin = "C:\Program Files\Git\bin"
.$gitBin\git.exe config --system core.longpaths true
# Write-Host "Git installed"

Import-PackageProvider Nuget -Force
install-Module CredentialManager -force

# Java
.$source\jdk-8u181-windows-x64.exe /s|Out-Null
# Write-Host "Java installed"

# OneScript
.$source\OneScript-1.0.21-setup.exe /silent|Out-Null
# Write-Host "OScript installed"

# TightVNC
msiexec /i $source\tightvnc-2.8.11-gpl-setup-64bit.msi /quiet /norestart ADDLOCAL="Server,Viewer" SERVER_REGISTER_AS_SERVICE=1 SERVER_ADD_FIREWALL_EXCEPTION=1 VIEWER_ADD_FIREWALL_EXCEPTION=1 SET_USECONTROLAUTHENTICATION=1 VALUE_OF_USECONTROLAUTHENTICATION=0 SET_USEVNCAUTHENTICATION=1 VALUE_OF_USEVNCAUTHENTICATION=0 SET_ALLOWLOOPBACK=1 VALUE_OF_ALLOWLOOPBACK=1

# Far
msiexec /i $source\Far30b5254.x64.20180805.msi /quiet|Out-Null
# Write-Host "FAR installed"

# 7z
.$source\7z1801-x64.exe /S|Out-Null
# Write-Host "7Z installed"

# VSCode
New-Item -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode
New-Item -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode\command
New-ItemProperty -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode -Name "(default)" -Value "Open wiith Code"
New-ItemProperty -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode -Name "Icon" -Value "C:\Program Files\Microsoft VS Code\Code.exe"
New-ItemProperty -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode\command -Name "(default)" -Value "C:\Program Files\Microsoft VS Code\Code.exe %V"

# Google Chrome
$LocalTempDir = $env:TEMP;
$ChromeInstaller = "ChromeInstaller.exe";
(new-object System.Net.WebClient).DownloadFile('http://dl.google.com/chrome/install/375.126/chrome_installer.exe', "$LocalTempDir\$ChromeInstaller");
& "$LocalTempDir\$ChromeInstaller" /silent /install;
$Process2Monitor =  "ChromeInstaller";
Do {
    $ProcessesFound = Get-Process | Where-Object{$_.Name -contains $Process2Monitor} | Select-Object -ExpandProperty Name;
    If ($ProcessesFound) {
        Write-Verbose "Still running: Installer" -Verbose;
        Start-Sleep -Seconds 2 }
    else {
        Remove-Item "$LocalTempDir\$ChromeInstaller" -ErrorAction SilentlyContinue -Verbose}
    }
Until (!$ProcessesFound)

# MS Office
.$source\Office2013x64standart\setup.exe /adminfile of2013.msp
Start-Sleep -Seconds 5
$Process2Monitor =  "setup";
Do {
    $ProcessesFound = Get-Process | Where-Object{$_.Name -contains $Process2Monitor} | Select-Object -ExpandProperty Name;
    If ($ProcessesFound) {
        Write-Verbose "Still running: Installer" -Verbose;
        Start-Sleep -Seconds 2 }
    else {
        Write-Verbose "Done!" -Verbose}
    }
Until (!$ProcessesFound)

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

# 1C install <----------
$v8version = "8.3.13.1644"
$user1c = "USR1CV8"
$Password1C = ConvertTo-SecureString "Dmd1234" -AsPlainText -Force
$mycreds = New-Object -TypeName System.Management.Automation.PSCredential -ArgumentList "$env:COMPUTERNAME\$user1c", $Password1C

New-LocalUser $user1c -Password $Password1C -FullName $user1c -PasswordNeverExpires -Description "User for 1C service"
Copy-Item "$source\ntrights.exe" -Destination $env:windir|Out-Null
$args1c = "-u USR1CV8 +r SeServiceLogonRight"
Start-Process "$env:windir\ntrights.exe" -ArgumentList $args1c

$dir1c = "$env:ProgramFiles\1cv8"
if((Test-Path -path $dir1c) -ne $true)
{
 New-Item -Path $dir1c -ItemType "directory"
 $acl = Get-Acl -Path $dir1c
 $rule=new-object System.Security.AccessControl.FileSystemAccessRule $user1c,"FullControl","ContainerInherit,ObjectInherit", "None","allow"
 $acl.AddAccessRule($rule)
 Set-Acl -Path $dir1c -AclObject $acl
 }

Copy-Item "$source\1C\8.3.13.1644\installed" -Destination $dir1c\$v8version -Recurse -Force

#DEVOPS-494 add -debug -http
New-Service -Name "1C:Enterprise 8.3 Server Agent (x86-64)" -BinaryPathName "C:\Program Files\1cv8\$v8version\bin\ragent.exe -srvc -agent -regport 1541 -port 1540 -range 1560:1591 -d ""D:\srvinfo"" -debug -http" -DisplayName "Агент сервера 1С:Предприятия 8.3 (x86-64)" -StartupType Automatic -Credential $mycreds
Start-Sleep -Seconds 5
Start-Service -Name "1C:Enterprise 8.3 Server Agent (x86-64)"

$rasName = "1C:Enterprise 8.3 Remote Server"
New-Service -Name $rasName -BinaryPathName "C:\Program Files\1cv8\$v8version\bin\ras.exe cluster --service" -DisplayName "1C:Enterprise 8.3 Remote Server" -StartupType Automatic -Credential $mycreds
Start-Sleep -Seconds 5
Start-Service -Name $rasName

#DEVOPS-491 registration comcntr.dll
$compath = "C:\Program Files\1cv8\$v8version\bin\comcntr.dll"
regsvr32 $compath

# 1C client
$distrpath = "$source\1C\$v8version\windows64full"
$msiname = "1CEnterprise 8 (x86-64).msi"
$args1client = " /qn /passive TRANSFORMS=adminstallrelogon.mst;1049.mst DESIGNERALLCLIENTS=1 THICKCLIENT=1 THINCLIENTFILE=1 THINCLIENT=1 SERVERCLIENT=1 WEBSERVEREXT=1 LANGUAGES=RU"
$dest = "$env:TEMP\1C"
Copy-Item $distrpath -Destination $dest -Recurse 
Start-Process -Wait $dest\$msiname -ArgumentList $args1client
Remove-Item $dest -Recurse -Force
Set-Location "C:\Program Files\1cv8\$v8version\bin\"
Start-Process RegMSC.cmd
#------------------->

# DEVOPS-492
Invoke-Sqlcmd -Username "sa" -Password $sapwd -Query "
CREATE LOGIN [bit-erp\jenkins] FROM WINDOWS;
CREATE LOGIN [$env:COMPUTERNAME\USR1CV8] FROM WINDOWS;
GO
EXEC sp_addsrvrolemember '$env:COMPUTERNAME\USR1CV8', 'sysadmin';  
EXEC sp_addsrvrolemember 'bit-erp\jenkins', 'sysadmin';
GO"

#1C licence
Copy-Item $share\1C\HASP\ -Destination "C:\Program Files\1cv8\$v8version\bin\" -Recurse
$pathHasp = "C:\Program Files\1cv8\$v8version\bin\1c8_UP.exe C:\Program Files\1cv8\$v8version\bin\backbas.dll"
invoke-expression $pathHasp

#DEVOPS-559
$FileConf = "C:\Program Files\1cv8\conf\conf.cfg"
Set-content  "SystemLanguage=RU" $FileConf
"DisableUnsafeActionProtection=.*" | Out-File $FileConf -append

# DEVOPS-701 install v8runner
function RunConsole($scriptBlock)
{
    # Исправление: Exception setting "OutputEncoding": "The handle is invalid."
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
$arguments = "-jar $jenkinsServiceDirectory\swarm-client-3.9.jar -master http://172.16.10.14:8080/ -username jenkins -password ""^2kkR92mS)RS"" -executors 3 -labels $label -fsroot $fsroot"
& $nssmPath install jenkins-service $javaPath
& $nssmPath set jenkins-service AppParameters $arguments
& $nssmPath start jenkins-service

# jenkins-slave interactive
$user = "bit-erp\jenkins"
$password = "^2kkR92mS)RS"
$connectorName = "jenkins-start-interactive.ps1"
$taskName = "jenkins-start"
$shell = "powershell.exe"
$logon_path = 'Registry::HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon'
Set-ItemProperty -Path $logon_path -Name "AutoAdminLogon" -Value 1
Set-ItemProperty -Path $logon_path -Name "DefaultUserName" -Value "$user"
Set-ItemProperty -Path $logon_path -Name "DefaultPassword" -Value $password
Set-ItemProperty -Path $logon_path -Name "ForceAutoLogon" -Value 1


$jenkinsConnnectDirectory = "D:\jenkins\interactive\connect"
$jenkinsWorkDirectory = "D:\jenkins\interactive\data"

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
'$fsroot = "D:\jenkins\interactive\data"' + "`r`n" +
'$args = "-jar swarm-client-3.9.jar -master http://172.16.10.14:8080/ -username jenkins -password ""^2kkR92mS)RS"" -executors 3 -labels $label -fsroot $fsroot"' + "`r`n" +
'Start-Process java -ArgumentList $args') -FilePath $startFile
Copy-Item "$jenkinsSource\swarm-client-3.9.jar" -Destination $jenkinsConnnectDirectory


$Trigger= New-ScheduledTaskTrigger -AtLogOn -User $User
$Action= New-ScheduledTaskAction -Execute $shell -Argument "-File $jenkinsConnnectDirectory\$connectorName" -WorkingDirectory $jenkinsConnnectDirectory
Register-ScheduledTask -TaskName $taskName -Trigger $Trigger -User $User -Action $Action

#DEVOPS-558
$jreVersion = (Get-ChildItem "$env:ProgramFiles\java\" -Exclude "jdk*").Name
$javaPath = "$env:ProgramFiles\java\$jreVersion\bin\"
[Environment]::SetEnvironmentVariable(
    "Path",
    [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::Machine) + ";$javaPath",
    [EnvironmentVariableTarget]::Machine)
	
[Environment]::SetEnvironmentVariable(
    "JAVA_HOME",
    "$javaPath",
    [EnvironmentVariableTarget]::Machine)

shutdown /r /t 1

