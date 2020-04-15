$paths = "C:\jenkins","$env:Homedir\Users\jenkis"
Foreach ($pathex in $paths){
Add-MpPreference -ExclusionPath $pathex
}