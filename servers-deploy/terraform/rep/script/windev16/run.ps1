
 param (
    [string]$user = "admin",
    [string]$password = "Lexroot12qwe"
)

# Disable ipv6
New-ItemProperty -Path HKLM:\SYSTEM\CurrentControlSet\services\TCPIP6\Parameters -Name DisabledComponents -PropertyType DWord -Value 0xff

# Отключаем Конфигурацию усиленной безопасности IE
$AdminKey = "HKLM:\SOFTWARE\Microsoft\Active Setup\Installed Components\{A509B1A7-37EF-4b3f-8CFC-4F3A74704073}"
    $UserKey = "HKLM:\SOFTWARE\Microsoft\Active Setup\Installed Components\{A509B1A8-37EF-4b3f-8CFC-4F3A74704073}"
    Set-ItemProperty -Path $AdminKey -Name "IsInstalled" -Value 0
    Set-ItemProperty -Path $UserKey -Name "IsInstalled" -Value 0

# RDS-Services
Install-WindowsFeature -Name Remote-Desktop-Services,RDS-Licensing,RDS-RD-Server
Install-WindowsFeature -Name RSAT-RDS-Tools -IncludeAllSubFeature

# IIS
Install-WindowsFeature -Name Web-Server,Web-Basic-Auth,Web-Windows-Auth,Web-Health,Web-Http-Logging,Web-Request-Monitor,Web-Log-Libraries,Web-Http-Tracing,Web-Common-Http,Web-Default-Doc,Web-Dir-Browsing,Web-Http-Errors,Web-Static-Content,Web-Http-Redirect,Web-Performance,Web-App-Dev,Web-Mgmt-Tools,Web-WebSockets,Web-ISAPI-Ext,Web-ISAPI-Filter,Web-AppInit

# .Net 4.7.1
\\172.16.50.38\share\Distr\NDP471-KB4033342-x86-x64-AllOS-ENU.exe /q /norestart|Out-Null

# Устанавливаем план питания "Высокая производительность"
$p=Get-CimInstance -Name root\cimv2\power -Class win32_PowerPlan -Filter "ElementName='Высокая производительность'"
Invoke-CimMethod -InputObject $p -MethodName Activate

$SecPwd = $password # convertTo-Securestring “$password” –AsPlainText –Force

Get-Random -count 8 -InputObject (48..57 + 65..90 + 97..122) | ForEach-Object -begin { $sapwd = '' } -process {$sapwd += [char]$_} -end {$sapwd}
# Отправляем пароль sa в slack канал 996_access
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
$SqlServerIsoImagePath = "\\172.16.50.38\share\ISO\sql.iso"

#Mount the installation media, and change to the mounted Drive.
$mountVolume = Mount-DiskImage -ImagePath $SqlServerIsoImagePath -PassThru
$driveLetter = ($mountVolume | Get-Volume).DriveLetter
$drivePath = $driveLetter + ":"
push-location -path "$drivePath"

#.\Setup.exe /q /ACTION=Install /FEATURES=SQLEngine, LocalDB /UpdateEnabled /UpdateSource=MU /X86=false /INSTANCENAME=MSSQLSERVER /INSTALLSHAREDDIR="C:\Program Files\Microsoft SQL Server" /INSTALLSHAREDWOWDIR="C:\Program Files (x86)\Microsoft SQL Server" /SQLSVCINSTANTFILEINIT="True" /INSTANCEDIR="C:\Program Files\Microsoft SQL Server" /AGTSVCACCOUNT="NT Service\SQLSERVERAGENT" /AGTSVCSTARTUPTYPE="Manual" /SQLSVCSTARTUPTYPE="Automatic" /SQLCOLLATION="SQL_Latin1_General_CP1_CI_AS" /SQLSVCACCOUNT="NT Service\MSSQLSERVER" /SECURITYMODE="SQL"  /SQLSYSADMINACCOUNTS="$currentUserName" /IACCEPTSQLSERVERLICENSETERMS /SAPWD="$SecPwd" 
.\Setup.exe /q /ACTION=Install /FEATURES=SQLEngine, LocalDB /TCPENABLED="1" /UpdateEnabled=false /X86=false /INSTANCENAME=MSSQLSERVER /INSTALLSHAREDDIR="C:\Program Files\Microsoft SQL Server" /INSTALLSHAREDWOWDIR="C:\Program Files (x86)\Microsoft SQL Server" /SQLSVCINSTANTFILEINIT="True" /INSTANCEDIR="C:\Program Files\Microsoft SQL Server" /SQLBACKUPDIR="D:\SQL\BACKUP" /SQLUSERDBDIR="D:\SQL\DATA" /AGTSVCACCOUNT="NT Service\SQLSERVERAGENT" /AGTSVCSTARTUPTYPE="Manual" /SQLSVCSTARTUPTYPE="Automatic" /SQLCOLLATION="Cyrillic_General_CI_AS" /SQLSVCACCOUNT="NT Service\MSSQLSERVER" /SECURITYMODE="SQL"  /SQLSYSADMINACCOUNTS="$currentUserName" /IACCEPTSQLSERVERLICENSETERMS /SAPWD="$sapwd" 

#Dismount the installation media.
pop-location
Dismount-DiskImage -ImagePath $SqlServerIsoImagePath

#print Time taken to execute
$sw.Stop()
"Sql install script completed in {0:c}" -f $sw.Elapsed;


$filepath="\\172.16.50.38\share\ISO\SSMS-Setup-ENU.exe"
 
# start the SSMS installer
write-host "Beginning SSMS 2016 install..." -nonewline
$Parms = " /Install /Quiet /Norestart /Logs log.txt"
$Prms = $Parms.Split(" ")
& "$filepath" $Prms | Out-Null
Write-Host "SSMS installation complete" -ForegroundColor Green


# recovery Simple by default
Import-Module -Name SqlServer
Invoke-Sqlcmd -Username "sa" -Password $sapwd -Query "USE master; ALTER DATABASE model SET RECOVERY SIMPLE;" 

# VS Code satup
\\172.16.50.38\share\Distr\VSCodeSetup-x64-1.26.0 /verysilent|Out-null
Write-Host "VS Code installed"

# VC Redist
\\172.16.50.38\share\Distr\vcredist_x86_13.exe /install /quiet|Out-Null
\\172.16.50.38\share\Distr\vcredist_x64_13.exe /install /quiet|Out-Null
Write-Host "VC redist installed"

msiexec /i \\172.16.50.38\share\Distr\msodbcsql.msi /passive IACCEPTMSODBCSQLLICENSETERMS=YES |Out-Null
msiexec /i \\172.16.50.38\share\Distr\MsSqlCmdLnUtils.msi  /passive IACCEPTMSSQLCMDLNUTILSLICENSETERMS=YES |Out-Null

#Git
\\172.16.50.38\share\Distr\Git-2.18.0-64-bit.exe /verysilent|Out-Null
Write-Host "Git installed"

# Java
\\172.16.50.38\share\Distr\jdk-8u181-windows-x64.exe /s|Out-Null
Write-Host "Java installed"

# OneScript
\\172.16.50.38\share\Distr\OneScript-1.0.20-setup.exe /silent|Out-Null
Write-Host "OScript installed"

#Far
msiexec /i \\172.16.50.38\share\Distr\Far30b5254.x64.20180805.msi /quiet|Out-Null
Write-Host "FAR installed"

#7z
\\172.16.50.38\share\Distr\7z1801-x64.exe /S|Out-Null
Write-Host "7Z installed"

# Добавляем пункт контекстного меню отрыть папку с VSCode
New-Item -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode
New-Item -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode\command
New-ItemProperty -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode -Name "(default)" -Value "Open w&ith Code"
New-ItemProperty -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode -Name "Icon" -Value "C:\Program Files\Microsoft VS Code\Code.exe"
New-ItemProperty -Path HKLM:\SOFTWARE\Classes\Directory\shell\VSCode\command -Name "(default)" -Value "C:\Program Files\Microsoft VS Code\Code.exe %V"

# Ярлыки на общий рабочий стол
New-Item -ItemType SymbolicLink -Path "C:\Users\Public\Desktop" -Name "Far.lnk" -Value "C:\Program Files\Far Manager\Far.exe"
New-Item -ItemType SymbolicLink -Path "C:\Users\Public\Desktop" -Name "SourceTreeSetup-2.6.10.lnk" -Value "\\172.16.50.38\share\Distr\SourceTreeSetup-2.6.10.exe"

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
\\172.16.50.38\share\Distr\Office2013x64standart\setup.exe /adminfile of2013.msp
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
 
$domain = "bit-erp.loc"
$domainpwd = "s9z8Uj123" | ConvertTo-SecureString -asPlainText -Force
$domainuser = "$domain\admin" 
$domaincred = New-Object System.Management.Automation.PSCredential($domainuser,$domainpwd)
Add-Computer -DomainName $domain -Credential $domaincred
Add-LocalGroupMember -Group "Пользователи удаленного рабочего стола" -Member "bit-erp\dev"
shutdown /r /t 1

