$username = ""
$password = "" | ConvertTo-SecureString -asPlainText -Force
$credential = New-Object System.Management.Automation.PSCredential($username,$password)
Add-WindowsFeature -Name "RSAT-AD-PowerShell" –IncludeAllSubFeature
$pc = Get-ADComputer -Identity $env:COMPUTERNAME
$target = "OU=dev_vm,DC=bit-erp,DC=loc"
Move-ADObject $pc -TargetPath $target -Credential $credential
