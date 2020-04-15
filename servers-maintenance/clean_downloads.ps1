$usersfolder = Get-ItemPropertyValue "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\ProfileList" "ProfilesDirectory"
$profiles = Get-ChildItem $usersfolder

foreach ($name in $profiles) {
    $downloads = $usersfolder + "\" + $name + "\Downloads\"
    Get-ChildItem $downloads | where CreationTime -lt (Get-Date).AddHours(-48) | Remove-Item -recurse -force
}
