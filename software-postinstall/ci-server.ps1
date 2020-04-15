
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

# Добавляем пользователя для интерактивных тестов 
cmd.exe /c NET USER /ADD jenkins_console 2018bitERP

Start-Sleep -Seconds 2

# Autologon windows
$console_user = jenkins_console
$console_passw = "2018bitERP"
cmd.exe /c NET USER /ADD $console_user $console_passw
$logon_path = 'Registry::HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon'
Set-ItemProperty -Path $logon_path -Name "AutoAdminLogon" -Value 1
Set-ItemProperty -Path $logon_path -Name "DefaultUserName" -Value "$env:COMPUTERNAME\$console_user"
Set-ItemProperty -Path $logon_path -Name "DefaultPassword" -Value $console_passw
Set-ItemProperty -Path $logon_path -Name "ForceAutoLogon" -Value 1

# VNC-Server installation
msiexec /i \\172.16.50.38\share\Distr\tightvnc-2.8.11-gpl-setup-64bit.msi /quiet /norestart ADDLOCAL="Server,Viewer" SERVER_REGISTER_AS_SERVICE=1 SERVER_ADD_FIREWALL_EXCEPTION=1 VIEWER_ADD_FIREWALL_EXCEPTION=1 SET_USECONTROLAUTHENTICATION=1 VALUE_OF_USECONTROLAUTHENTICATION=0 SET_USEVNCAUTHENTICATION=1 VALUE_OF_USEVNCAUTHENTICATION=0 SET_ALLOWLOOPBACK=1 VALUE_OF_ALLOWLOOPBACK=1

# Отключаем Конфигурацию усиленной безопасности IE
$AdminKey = "HKLM:\SOFTWARE\Microsoft\Active Setup\Installed Components\{A509B1A7-37EF-4b3f-8CFC-4F3A74704073}"
    $UserKey = "HKLM:\SOFTWARE\Microsoft\Active Setup\Installed Components\{A509B1A8-37EF-4b3f-8CFC-4F3A74704073}"
    Set-ItemProperty -Path $AdminKey -Name "IsInstalled" -Value 0
    Set-ItemProperty -Path $UserKey -Name "IsInstalled" -Value 0


# .Net 4.7.1
\\172.16.50.38\share\Distr\NDP471-KB4033342-x86-x64-AllOS-ENU.exe /q /norestart|Out-Null

# Устанавливаем план питания "Высокая производительность"
$p=Get-CimInstance -Name root\cimv2\power -Class win32_PowerPlan -Filter "ElementName='Высокая производительность'"
Invoke-CimMethod -InputObject $p -MethodName Activate

$SecPwd = $password # convertTo-Securestring “$password” –AsPlainText –Force

# 1C
#\\172.16.50.38\share\Distr\1C\netinst\1CEStart.exe

# 1C EDT 
\\172.16.50.38\share\Distr\1c_enterprise_development_tools_distr_1.9.2_53_windows_x86_64\1ce-installer-cli.cmd install --ignore-hardware-checks |Out-null

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

\\172.16.50.38\share\Distr\git-lfs-windows-v2.5.2.exe /verysilent|Out-Null
Write-Host "Git lfs installed!"

# Java
\\172.16.50.38\share\Distr\jdk-8u181-windows-x64.exe /s|Out-Null
Write-Host "Java installed"

# OneScript
\\172.16.50.38\share\Distr\OneScript-1.0.20-setup.exe /silent|Out-Null
Write-Host "OScript installed"

opm install gitsync
opm install vanessa-runner
opm install add


#Far
msiexec /i \\172.16.50.38\share\Distr\Far30b5254.x64.20180805.msi /quiet|Out-Null
Write-Host "FAR installed"

#7z
\\172.16.50.38\share\Distr\7z1801-x64.exe /S|Out-Null
Write-Host "7Z installed"

# Nircmd
Copy-Item \\172.16.50.38\share\Distr\nircmd-x64\* -Destination $env:windir

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

 

