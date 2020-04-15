$regPath = (Get-ItemProperty 'HKLM:\SYSTEM\CurrentControlSet\Services\1C:Enterprise 8.3 Server Agent (x86-64)').Imagepath
$v8version = $regPath.Split("\")[3]
$path = "C:\Program Files\1cv8\$v8version\bin\comcntr.dll"
$path
regsvr32 $path