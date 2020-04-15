$ras = "1C:Enterprise 8.3 Remote Server"
$regPath = (Get-ItemProperty 'HKLM:\SYSTEM\CurrentControlSet\Services\1C:Enterprise 8.3 Server Agent (x86-64)').Imagepath

$rasExist = Get-Service|Where-Object {$_.Name -eq $ras}
if ($null -ne $rasExist){
    Write-Host "RAS-service exist"
    Stop-Service -Name $ras
    cmd.exe /c sc delete $ras
}

$v8version = $regPath.Split("\")[3]

$secpasswd = ConvertTo-SecureString "Dmd1234" -AsPlainText -Force
$mycreds = New-Object System.Management.Automation.PSCredential ("$env:COMPUTERNAME\USR1CV8", $secpasswd) 
New-Service -Name $ras -BinaryPathName "C:\Program Files\1cv8\$v8version\bin\ras.exe cluster --service" -DisplayName "1C:Enterprise 8.3 Remote Server" -StartupType Automatic -Credential $mycreds
Start-Sleep -Seconds 5
Start-Service -Name $ras