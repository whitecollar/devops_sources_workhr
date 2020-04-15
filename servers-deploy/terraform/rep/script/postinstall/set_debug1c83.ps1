$debug = Get-ItemProperty 'HKLM:\SYSTEM\CurrentControlSet\Services\1C:Enterprise 8.3 Server Agent (x86-64)'
$debugset = $debug.Imagepath + " -debug -http"
Set-ItemProperty 'HKLM:\SYSTEM\CurrentControlSet\Services\1C:Enterprise 8.3 Server Agent (x86-64)' -Name "ImagePath" -Value $debugset
Get-Service|Where {$_.Name -eq "1C:Enterprise 8.3 Server Agent (x86-64)"}|Restart-Service  