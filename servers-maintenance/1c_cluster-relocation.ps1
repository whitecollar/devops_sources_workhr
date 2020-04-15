# comments will be added soon

$newpath = "F:\srvinfo"

$oldvalue = Get-ItemPropertyValue "HKLM:\SYSTEM\CurrentControlSet\services\1C:Enterprise 8.3 Server Agent (x86-64)" "ImagePath"
#$regex = [regex]'(?<=-d ).*?(?= -debug)'
#$oldpath = $regex.Match($oldvalue);
$oldpath = $oldvalue.split("""")[3]
#$newpathreg = '"'+$newpath+'"'
$newvalue = $oldvalue.replace($oldpath,$newpath)

Get-Service | Where-Object {$_.Name -eq "1C:Enterprise 8.3 Server Agent (x86-64)"} | Stop-Service
Start-Sleep -seconds 20
$procs = Get-Process | Where-Object {($_.ProcessName -eq "ragent") -or ($_.ProcessName -eq "rphost") -or ($_.ProcessName -eq "rmngr")}

foreach ($proc in $procs){
    if ($proc.ID -ne $null){
        Stop-Process -InputObject $proc -Force
    }
}

Start-Sleep -seconds 10

Copy-Item -path "$oldpath" -destination $newpath -recurse -force

Set-ItemProperty "HKLM:\SYSTEM\CurrentControlSet\services\1C:Enterprise 8.3 Server Agent (x86-64)" -name ImagePath -value "$newvalue"

Get-ChildItem -path "$newpath\reg_1541\snccntx*" | Get-ChildItem | Remove-Item -force
Start-Service "1C:Enterprise 8.3 Server Agent (x86-64)"
Get-Service | Where-Object {$_.Name -eq "1C:Enterprise 8.3 Server Agent (x86-64)"}
