#
# 1. скрипт должен запускаться на машине share.bit-erp.loc
# 2. в строке запуска скрипта передаём логин нового пользователя, например:
#    new_worker_folders_creation.ps1 dmaksimov
#

param ([string]$ADUserLogin)

$path = "E:\Users"
$folder = $ADUserLogin.tolower()

New-Item -path "$path\$folder" -type directory

# отключаем наследование и даём полные права пользователю на свою папку

$ACL = Get-ACL "$path\$folder"
$ACL.SetAccessRuleProtection($true,$true)
$ACL | Set-Acl "$path\$folder"

$rule = New-Object System.Security.AccessControl.FileSystemAccessRule("bit-erp\$ADUserLogin","FullControl","ContainerInherit,ObjectInherit","None","Allow")
$ACL = Get-ACL "$path\$folder"
$ACL.SetAccessRule($rule)
Set-ACL "$path\$folder" -AclObject $ACL
