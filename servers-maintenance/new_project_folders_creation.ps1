#
# Скрипт переводит новые группы AD по новому проекту в тип 'группа безопасности', создаёт папку проекта на диске и 
# папку пользовательского обмена внутри этой папки проекта, назначает папкам права доступа (по соглашению), создаёт
# группу # в Cerberus FTP с привязанной папкой проекта (для привязки сотрудников заказчика)
#
# 1. соответствующие группы проекта в AD должны быть созданы ПЕРЕД запуском скрипта;
# 2. скрипт должен запускаться на машине share.bit-erp.loc на сервисной ноде Jenkins;
# 3. в строке запуска передаём ключ нового проекта и имя + пароль пользователя bit-erp\jira-app
#    из кредсов Jenkins, например: 
#
#    new_project_folders_creation.ps1 TEST bit-erp\jira-app c00LPaSSW0Rd
#

param ([string]$ProjectKey, [string]$JiraAppUser, [string]$JiraAppUserPass)

$path = "E:\Projects"
$url = 'http://localhost:10001/service/cerberusftpservice'
$ContentType = "text/xml"

$mainGroup = Get-ADGroup "CN=$ProjectKey,OU=jira-biterp,DC=bit-erp,DC=loc"
$mainGroupSAM = $mainGroup.SAMAccountName

$ROGroupName = $ProjectKey + "_ro"
$ROGroup = Get-ADGroup "CN=$ROGroupName,OU=jira-biterp,DC=bit-erp,DC=loc"
$ROGroupSAM = $ROGroup.SAMAccountName

# изменяем тип группы в AD для проекта из 'распространения' в 'безопасность'
#
$user = "$JiraAppUser"
$pass = "$JiraAppUserPass"
$password = ConvertTo-SecureString -asPlainText $pass -force
$cred = New-Object -TypeName System.Management.Automation.PSCredential -ArgumentList $user, $password
$credential = Get-Credential $cred

$session = New-PSSession -ComputerName erpad.bit-erp.loc -Credential $credential
 
$script = 
{
    param ([string]$ProjectKey)
    $ROGroupName = $ProjectKey + "_ro"

    Get-ADGroup "CN=$ProjectKey,OU=jira-biterp,DC=bit-erp,DC=loc" | Set-ADGroup -GroupCategory Security
    Get-ADGroup "CN=$ROGroupName,OU=jira-biterp,DC=bit-erp,DC=loc" | Set-ADGroup -GroupCategory Security
}
 
#Start-Job -Name Test -ArgumentList $ProjectKey -ScriptBlock $script -credential $credential | Wait-Job | Receive-Job
#Remove-Job -Name Test

Invoke-Command -Session $session -ScriptBlock $script -ArgumentList $ProjectKey

Start-Sleep 5

# создаём основную папку проектов и папку обмена (!Обмен в cp1251) внутри неё
#
New-Item -path "$path\$ProjectKey\" -type directory
New-Item -path "$path\$ProjectKey\Share" -type directory

# отключаем наследование для основной папки
#
$ACL = Get-ACL "$path\$ProjectKey"
$ACL.SetAccessRuleProtection($true,$true)
$ACL | Set-Acl "$path\$ProjectKey"

# отключаем наследование для папки обмена
#
$ACL = Get-ACL "$path\$ProjectKey\Share"
$ACL.SetAccessRuleProtection($true,$true)
$ACL | Set-Acl "$path\$ProjectKey\Share"

# даём полные права к основной папке основной группе проекта (группа в AD = <ключ проекта>)
#
$rule = New-Object System.Security.AccessControl.FileSystemAccessRule("bit-erp\$mainGroupSAM","FullControl","ContainerInherit,ObjectInherit","None","Allow")
$ACL = Get-ACL "$path\$ProjectKey"
$ACL.SetAccessRule($rule)
Set-ACL "$path\$ProjectKey" -AclObject $ACL

# даём полные права к папке обмена основной группе проекта (группа в AD = <ключ проекта>)
#
$rule = New-Object System.Security.AccessControl.FileSystemAccessRule("bit-erp\$mainGroupSAM","FullControl","ContainerInherit,ObjectInherit","None","Allow")
$ACL = Get-ACL "$path\$ProjectKey\Share"
$ACL.SetAccessRule($rule)
Set-ACL "$path\$ProjectKey\Share" -AclObject $ACL

# даём права для чтения основной папки проекта группе 'для чтения' (группа в AD = <ключ проекта>_ro)
#
$rule = New-Object System.Security.AccessControl.FileSystemAccessRule("bit-erp\$ROGroupSAM","ReadAndExecute","ContainerInherit,ObjectInherit","None","Allow")
$ACL = Get-ACL "$path\$ProjectKey"
$ACL.SetAccessRule($rule)
Set-ACL "$path\$ProjectKey" -AclObject $ACL

# даём полные права группе 'для чтения' (группа в AD = <ключ проекта>_ro) для папки обмена (внутри основной папки проекта)
#
$rule = New-Object System.Security.AccessControl.FileSystemAccessRule("bit-erp\$ROGroupSAM","FullControl","ContainerInherit,ObjectInherit","None","Allow")
$ACL = Get-ACL "$path\$ProjectKey\Share"
$ACL.SetAccessRule($rule)
Set-ACL "$path\$ProjectKey\Share" -AclObject $ACL

# создаём группу в Cerberus FTP для привязки внешних пользователей проекта
#
$CerberusGroupName = $ProjectKey.tolower() + "_ro"

$soap = [xml]@"
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:cer="http://cerberusllc.com/service/cerberusftpservice" xmlns:com="http://cerberusllc.com/common">
   <soap:Header/>
   <soap:Body>
      <cer:AddGroupRequest>
         <com:credentials>
            <com:user>admin</com:user>
            <com:password>GwP6Ykp1Bw</com:password>
         </com:credentials>
         <cer:Group com:name="$CerberusGroupName">
            <com:isAllowPasswordChange com:value="false"/>
            <com:isSimpleDirectoryMode com:value="true"/>
            <com:maxLoginsAllowed com:value="100" />
            <com:protocols com:ftps="true" com:https="true" />
            <com:rootList>
               <com:root>
                  <com:name>$ProjectKey</com:name>
                  <com:path>E:\Projects\$ProjectKey</com:path>
                  <com:permissions>
                     <com:allowListFile>true</com:allowListFile>
                     <com:allowListDir>true</com:allowListDir>
                     <com:allowDownload>true</com:allowDownload>
                     <com:allowUpload>true</com:allowUpload>
                     <com:allowRename>true</com:allowRename>
                     <com:allowDelete>true</com:allowDelete>
                     <com:allowDirectoryCreation>true</com:allowDirectoryCreation>
                     <com:allowZip>true</com:allowZip>
                     <com:allowUnzip>true</com:allowUnzip>
                  </com:permissions>
               </com:root>
            </com:rootList>
         </cer:Group>
      </cer:AddGroupRequest>
   </soap:Body>
</soap:Envelope>
"@

Invoke-WebRequest -Uri $url -Method Post -Body $soap -ContentType $ContentType -UseBasicParsing
