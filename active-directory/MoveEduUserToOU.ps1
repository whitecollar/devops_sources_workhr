$targetOU = "OU=EDU_users,OU=jira-biterp,DC=bit-erp,DC=loc"
Get-ADGroupMember '$G52000-KKULOMVLID3U'  | Where-Object DistinguishedName -notlike "*$targetOU" | Move-ADObject -TargetPath $targetOU