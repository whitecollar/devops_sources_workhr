$v8version = "8.3.13.1644"
$source = "\\share\soft"
$user1c = "USR1CV8"
$Password = ConvertTo-SecureString "Dmd1234" -AsPlainText -Force
$mycreds = New-Object -TypeName System.Management.Automation.PSCredential -ArgumentList "$env:COMPUTERNAME\$user1c", $Password

New-LocalUser $user1c -Password $Password -FullName $user1c -PasswordNeverExpires -Description "User for 1C service"
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

New-Service -Name "1C:Enterprise 8.3 Server Agent (x86-64)" -BinaryPathName "C:\Program Files\1cv8\$v8version\bin\ragent.exe -srvc -agent -regport 1541 -port 1540 -range 1560:1591 -d ""D:\srvinfo"" -debug -http" -DisplayName "Агент сервера 1С:Предприятия 8.3 (x86-64)" -StartupType Automatic -Credential $mycreds
Start-Sleep -Seconds 5
Start-Service -Name "1C:Enterprise 8.3 Server Agent (x86-64)"

$ras = "1C:Enterprise 8.3 Remote Server"
New-Service -Name $ras -BinaryPathName "C:\Program Files\1cv8\$v8version\bin\ras.exe cluster --service" -DisplayName "1C:Enterprise 8.3 Remote Server" -StartupType Automatic -Credential $mycreds
Start-Sleep -Seconds 5
Start-Service -Name $ras

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