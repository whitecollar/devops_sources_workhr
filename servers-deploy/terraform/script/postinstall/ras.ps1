$v8ver = "8.3.12.1529"
$secpasswd = ConvertTo-SecureString "Dmd1234" -AsPlainText -Force
$mycreds = New-Object System.Management.Automation.PSCredential ("$env:COMPUTERNAME\USR1CV8", $secpasswd) 
New-Service -Name "1C:Enterprise 8.3 Remote Server" -BinaryPathName "C:\Program Files\1cv8\$v8ver\bin\ras.exe cluster --service" -DisplayName "1C:Enterprise 8.3 Remote Server" -StartupType Automatic -Credential $mycreds
Start-Sleep -Seconds 5
Start-Service -Name "1C:Enterprise 8.3 Remote Server"