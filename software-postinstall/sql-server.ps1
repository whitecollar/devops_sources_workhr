<<<<<<< HEAD
$net = new-object -ComObject WScript.Network
$net.MapNetworkDrive("r:", "\\share\soft", $false, "bit-erp\admin", "s9z8Uj123")
#$share = "\\share\soft"
$share = "r:\"
#$share = "\\share\soft"
=======

$share = "\\share\soft"
>>>>>>> 22ee5beac3343b3d0dd728d9e56acfbc4e2b6c85
$adminKey = "HKLM:\SOFTWARE\Microsoft\Active Setup\Installed Components\{A509B1A7-37EF-4b3f-8CFC-4F3A74704073}"
    $userKey = "HKLM:\SOFTWARE\Microsoft\Active Setup\Installed Components\{A509B1A8-37EF-4b3f-8CFC-4F3A74704073}"
    Set-ItemProperty -Path $adminKey -Name "IsInstalled" -Value 0
    Set-ItemProperty -Path $userKey -Name "IsInstalled" -Value 0


# .Net 4.7.1
.$share\NDP471-KB4033342-x86-x64-AllOS-ENU.exe /q /norestart|Out-Null

<<<<<<< HEAD
#Get-Random -count 3 -InputObject (65..90) | ForEach-Object -begin { $pwd1 = '' } -process {$pwd1 += [char]$_}
#Get-Random -count 3 -InputObject (97..122) | ForEach-Object -begin { $pwd2 = '' } -process {$pwd2 += [char]$_}
#Get-Random -count 2 -InputObject (48..57) | ForEach-Object -begin { $pwd3 = '' } -process {$pwd3 += [char]$_}
$sapwd =  $pwd1 + $pwd2 + $pwd3
$sapwd = $args[0]
echo $sapwd
=======
Get-Random -count 3 -InputObject (65..90) | ForEach-Object -begin { $pwd1 = '' } -process {$pwd1 += [char]$_}
Get-Random -count 3 -InputObject (97..122) | ForEach-Object -begin { $pwd2 = '' } -process {$pwd2 += [char]$_}
Get-Random -count 2 -InputObject (48..57) | ForEach-Object -begin { $pwd3 = '' } -process {$pwd3 += [char]$_}
$sapwd =  $pwd1 + $pwd2 + $pwd3

>>>>>>> 22ee5beac3343b3d0dd728d9e56acfbc4e2b6c85
Add-Type -AssemblyName System.Web
$url='https://hooks.slack.com/services/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'
$ip = get-WmiObject Win32_NetworkAdapterConfiguration
$ipaddr= $ip.ipaddress[0]
Add-Type -AssemblyName System.Net.Http
$http = New-Object -TypeName System.Net.Http.Httpclient
$message = "Server: " +$ipaddr + " sa password: " + $sapwd 
$httpMessage = "{""text"": """ + $message + """}";
$content = New-Object -TypeName System.Net.Http.StringContent($httpMessage)
$http.PostAsync("$url", $content).ResultHost

$sw = [Diagnostics.Stopwatch]::StartNew()
$currentUserName = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name;
$sqlServerIsoImagePath = "$share\ISO\sql.iso"

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


$filepath="$share\ISO\SSMS-Setup-ENU.exe"
 
# start the SSMS installer
write-host "Beginning SSMS 2016 install..." -nonewline
$Parms = " /Install /Quiet /Norestart /Logs log.txt"
$Prms = $Parms.Split(" ")
& "$filepath" $Prms | Out-Null
Write-Host "SSMS installation complete" -ForegroundColor Green


# recovery Simple by default
Import-Module -Name SqlServer
Invoke-Sqlcmd -Username "sa" -Password $sapwd -Query "USE master; ALTER DATABASE model SET RECOVERY SIMPLE;" 

# VC Redist
.$share\vcredist_x86_13.exe /install /quiet|Out-Null
.$share\vcredist_x64_13.exe /install /quiet|Out-Null
Write-Host "VC redist installed"

msiexec /i $share\msodbcsql.msi /passive IACCEPTMSODBCSQLLICENSETERMS=YES |Out-Null
msiexec /i $share\MsSqlCmdLnUtils.msi  /passive IACCEPTMSSQLCMDLNUTILSLICENSETERMS=YES |Out-Null


# 7z
.$share\7z1801-x64.exe /S|Out-Null
# Write-Host "7Z installed"

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


#devops-221 add instal zabbix-agent ver 4.0.0
New-Item -Path 'c:\Tools\Zabbix\' -ItemType Directory
Copy-Item $share\Zabbix\zabbix_agents-4.0.0-win-amd64\bin\zabbix_agentd.exe c:\Tools\Zabbix\zabbix_agentd.exe
Copy-Item $share\Zabbix\zabbix_agentd.win.conf c:\Tools\Zabbix\zabbix_agentd.win.conf
C:\Tools\Zabbix\zabbix_agentd.exe -i -c C:\Tools\Zabbix\zabbix_agentd.win.conf
Start-Service "Zabbix Agent"

# consul
$consulDistr = "$share\consul\windows"
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
 
# Jenkins-slave service
$source = "\\share\soft\scripts\jenkins-install"
$jenkinsServiceDirectory = "C:\jenkinsserviceconnect"
$jreVersion = (Get-ChildItem "$env:ProgramFiles\java\" -Exclude "jdk*").Name
$javaPath = "$env:ProgramFiles\java\$jreVersion\bin\java.exe"

if((Test-Path -path $jenkinsServiceDirectory) -ne $true)
{
 New-Item -Path $jenkinsServiceDirectory -ItemType "directory"
 }

Copy-Item $source\* -Destination $jenkinsServiceDirectory
$nssmPath = "$jenkinsServiceDirectory\nssm.exe"
$label = $env:COMPUTERNAME.ToLower()+"service"
$fsroot = "C:\jenkinsservice"
$arguments = "-jar $jenkinsServiceDirectory\swarm-client-3.9.jar -master http://172.16.10.14:8080/ -username jenkins -password ""^2kkR92mS)RS"" -executors 3 -labels $label -fsroot $fsroot"
& $nssmPath install jenkins-service $javaPath
& $nssmPath set jenkins-service AppParameters $arguments
& $nssmPath start jenkins-service