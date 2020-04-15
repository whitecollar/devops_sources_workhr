#Installs SQL Server locally with standard settings for Developers/Testers.
# Install SQL from command line help - https://msdn.microsoft.com/en-us/library/ms144259.aspx
 param (
    [string]$user = "admin1",
    [string]$password = "Lexroot12qwe"
)
#cmd.exe /c NET USER /ADD $user $password
#cmd.exe /c net localgroup Администраторы $user /add
#mkdir c:\install
#wget http://185.96.87.20/distr/sql.iso -OutFile c:\install\sql.iso
$file111 = "C:\111.txt"
echo "1" > $file111
$SecPwd = $password # convertTo-Securestring “$password” –AsPlainText –Force

$sw = [Diagnostics.Stopwatch]::StartNew()
$currentUserName = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name;
$SqlServerIsoImagePath = "\\172.16.50.38\share\ISO\sql.iso"

#Mount the installation media, and change to the mounted Drive.
$mountVolume = Mount-DiskImage -ImagePath $SqlServerIsoImagePath -PassThru
$driveLetter = ($mountVolume | Get-Volume).DriveLetter
$drivePath = $driveLetter + ":"
push-location -path "$drivePath"


echo "2" > $file111
#Install SQL Server locally with our default settings. 
# Only the Sql Engine and LocalDB
# i.e. no Replication, FullText, Data Quality, PolyBase, R, AnalysisServices, Reporting Services, Integration service, Master Data Services, Books Online(BOL) or SDK are installed.
.\Setup.exe /q /ACTION=Install /FEATURES=SQLEngine, LocalDB /UpdateEnabled /UpdateSource=MU /X86=false /INSTANCENAME=MSSQLSERVER /INSTALLSHAREDDIR="C:\Program Files\Microsoft SQL Server" /INSTALLSHAREDWOWDIR="C:\Program Files (x86)\Microsoft SQL Server" /SQLSVCINSTANTFILEINIT="True" /INSTANCEDIR="C:\Program Files\Microsoft SQL Server" /AGTSVCACCOUNT="NT Service\SQLSERVERAGENT" /AGTSVCSTARTUPTYPE="Manual" /SQLSVCSTARTUPTYPE="Automatic" /SQLCOLLATION="SQL_Latin1_General_CP1_CI_AS" /SQLSVCACCOUNT="NT Service\MSSQLSERVER" /SECURITYMODE="SQL"  /SQLSYSADMINACCOUNTS="$currentUserName" /IACCEPTSQLSERVERLICENSETERMS /SAPWD="$SecPwd" 

#Dismount the installation media.
pop-location
Dismount-DiskImage -ImagePath $SqlServerIsoImagePath

#print Time taken to execute
$sw.Stop()
"Sql install script completed in {0:c}" -f $sw.Elapsed;


# Set file and folder path for SSMS installer .exe
$folderpath="c:\install"
if (!(Test-Path $folderpath)){
New-Item -ItemType Directory -Path "C:\Install"
}
$filepath="$folderpath\SSMS-Setup-ENU.exe"
 
#If SSMS not present, download
if (!(Test-Path $filepath)){
write-host "Downloading SQL Server 2016 SSMS..."
$URL = "https://download.microsoft.com/download/3/1/D/31D734E0-BFE8-4C33-A9DE-2392808ADEE6/SSMS-Setup-ENU.exe"
$clnt = New-Object System.Net.WebClient
$clnt.DownloadFile($url,$filepath)
Write-Host "SSMS installer download complete" -ForegroundColor Green
 
}
else {
 
write-host "Located the SQL SSMS Installer binaries, moving on to install..."
}
 
# start the SSMS installer
write-host "Beginning SSMS 2016 install..." -nonewline
$Parms = " /Install /Quiet /Norestart /Logs log.txt"
$Prms = $Parms.Split(" ")
& "$filepath" $Prms | Out-Null
Write-Host "SSMS installation complete" -ForegroundColor Green
