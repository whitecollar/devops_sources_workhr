# Перевод журналов регистрации в старый формат
$paths = Get-ChildItem 'C:\Program Files\1cv8\srvinfo\reg_1541\*\1Cv8Log\'
Foreach ($path in $paths){
    if ((Test-Path "$path\1Cv8.lgd") -eq $true){
        New-Item "$path\1Cv8.lgf"
        Remove-Item "$path\1Cv8.lgd"
        Remove-Item "$path\1Cv8.lgd-journal"
        Get-ChildItem $path
        }
}