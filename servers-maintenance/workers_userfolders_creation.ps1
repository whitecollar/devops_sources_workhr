#
# создаём папки сотрудников, скрипт:
# 1) создаёт папки по названиям активных аккаунтов из группы сотрудников в AD (bit-erp.loc\biterp)
# 2) отключает наследование и назначат полные права на созданную папку для одноимённой учётной записи
# 
 
$usersFolderPath = "E:\Users\"

$accounts = Get-ADGroup -filter {(name -eq "biterp")} | Get-ADGroupMember -recursive | where { $_.objectClass -eq "user" } `
    | Get-ADUser -properties * | where {$_.enabled -eq $true} `
    | where {$_.lockedout -eq $false}  | select SamAccountName -unique
$accountList = $accounts.SAMAccountName.toLower()

foreach ($name in $accountList) {
    New-Item -path "$usersFolderPath\$name" -type directory

    $ACL = Get-ACL "$usersFolderPath\$name"
    $ACL.SetAccessRuleProtection($true,$true)
    $ACL | Set-Acl "$usersFolderPath\$name"

    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule("bit-erp\$name","FullControl","ContainerInherit,ObjectInherit","None","Allow")
    $ACL = Get-ACL "$usersFolderPath\$name"
    $ACL.SetAccessRule($rule)
    Set-ACL "$usersFolderPath\$name" -AclObject $ACL
}
