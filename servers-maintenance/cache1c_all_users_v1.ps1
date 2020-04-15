# Скрипт чистки кэша 1С всех пользователей на сервере

$users=Get-ChildItem -Path "C:\Users" -Exclude "USR1*"

# Для 8.3
foreach ($user in $users){
$path83local="$user\AppData\Local\1C\1cv8"
if((Test-Path $path83local) -eq "true" )
{get-childitem $path83local|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse} 

$path83roaming="$user\AppData\Roaming\1C\1cv8"
if((Test-Path $path83roaming) -eq "true" )
{get-childitem $path83roaming|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse}

# Для 8.2
$path82roaming="$user\AppData\Roaming\1C\1cv82"
if((Test-Path $path82roaming) -eq "true" )
{get-childitem $path82roaming|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse}

$path82local="$user\AppData\Local\1C\1cv82"
if((Test-Path $path82local) -eq "true" )
{get-childitem $path82local|where {$_.Name -match "........-....-....-....-............"}|remove-item -force -recurse}
}