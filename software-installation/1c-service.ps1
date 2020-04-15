$source = "\\share\soft"
$v8ver = "8.3.13.1690"
$distrpath = "$source\1C\$v8ver\windows64full"
$msiname = "1CEnterprise 8 (x86-64).msi"
$args = " /qn /passive TRANSFORMS=client.mst;1049.mst LANGUAGES=RU"
$dest = "$env:TEMP\1C"
$password = "Dmd1234"

Stop-Service -Name "1C:Enterprise 8.3 Remote Server"

cmd.exe /c NET USER /ADD USR1CV8 Dmd1234
cmd.exe /c sc delete "1C:Enterprise 8.3 Remote Server"

Copy-Item $distrpath -Destination $dest -Recurse 
Start-Process -Wait $dest\$msiname -ArgumentList $args
Remove-Item $dest -Recurse -Force
$secpasswd = ConvertTo-SecureString $password -AsPlainText -Force
$mycreds = New-Object System.Management.Automation.PSCredential ("$env:COMPUTERNAME\USR1CV8", $secpasswd) 
New-Service -Name "1C:Enterprise 8.3 Server Agent (x86-64)" -BinaryPathName "C:\Program Files\1cv8\$v8ver\bin\ragent.exe -srvc -agent -regport 1541 -port 1540 -range 1560:1591 -d ""D:\srvinfo"" -debug -http" -DisplayName "Агент сервера 1С:Предприятия 8.3 (x86-64)" -StartupType Automatic -Credential $mycreds
New-Service -Name "1C:Enterprise 8.3 Remote Server" -BinaryPathName "C:\Program Files\1cv8\$v8ver\bin\ras.exe cluster --service" -DisplayName "1C:Enterprise 8.3 Remote Server" -StartupType Automatic -Credential $mycreds
Start-Sleep -Seconds 5

Start-Service -Name "1C:Enterprise 8.3 Server Agent (x86-64)"
Start-Service -Name "1C:Enterprise 8.3 Remote Server"