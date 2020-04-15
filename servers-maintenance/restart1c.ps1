$ImagePath = Get-ItemPropertyValue "HKLM:\SYSTEM\CurrentControlSet\services\1C:Enterprise 8.3 Server Agent (x86-64)" "ImagePath"
$srvinfo = $ImagePath.Split("""")[3]

Get-Service|Where-Object {$_.Name -eq "1C:Enterprise 8.3 Server Agent (x86-64)"}|Stop-Service
Start-Sleep -Seconds 20
$procs = Get-Process|Where-Object {($_.ProcessName -eq "ragent") -or ($_.ProcessName -eq "rphost") -or ($_.ProcessName -eq "rmngr")}

foreach ($proc in $procs){
    if ($proc.ID -ne $null){
        Stop-Process -InputObject $proc -Force
    }
}

Start-Sleep -Seconds 10

# Очистка временных сессионных данных
Get-ChildItem -Path "$srvinfo\reg_1541\snccntx*"|Get-ChildItem|Remove-Item -Force
Start-Service "1C:Enterprise 8.3 Server Agent (x86-64)"
Get-Service|Where-Object {$_.Name -eq "1C:Enterprise 8.3 Server Agent (x86-64)"}
