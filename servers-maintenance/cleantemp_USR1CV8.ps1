$usersfolder = Get-ItemPropertyValue "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\ProfileList" "ProfilesDirectory" 
$path1 = $usersfolder + "\USR1CV8\Appdata\Local\Temp\"
$path2 = "F:\USR1CV8\temp"

Get-ChildItem -path $path1 -filter *.zip | where CreationTime -lt (Get-Date).AddMinutes(-15) | Remove-Item -force
Get-ChildItem -path $path2 -filter *.zip | where CreationTime -lt (Get-Date).AddMinutes(-15) | Remove-Item -force
