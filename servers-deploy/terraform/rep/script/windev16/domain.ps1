$ou= $args[0]
  
#Install-PackageProvider -Name NuGet -RequiredVersion 2.8.5.201 -Force
#Set-PSRepository -Name PSGallery -InstallationPolicy Trusted
#Get-ExecutionPolicy
#Set-ExecutionPolicy RemoteSigned -Scope Process -force
#Import-Module PSWindowsUpdate
#Install-Module -Name PSWindowsUpdate -RequiredVersion 2.0.0.4
#Get-WUInstall -KBArticleID KB4103723  -AcceptAll -IgnoreReboot
  
REG ADD "HKLM\Software\Microsoft\Windows\CurrentVersion\Policies\System\CredSSP\Parameters" /v AllowEncryptionOracle /t REG_DWORD /d 2
   
$domain = "bit-erp.loc"
$password = "s9z8Uj123" | ConvertTo-SecureString -asPlainText -Force
$username = "$domain\devopsservices" 
$credential = New-Object System.Management.Automation.PSCredential($username,$password)
Add-Computer -DomainName $domain -OUPath "OU=$ou,DC=bit-erp,DC=loc" -Credential $credential

# Java
$net = new-object -ComObject WScript.Network
$net.MapNetworkDrive("r:", "\\share\soft", $false, $username, "s9z8Uj123")
$source = "r:"
.$source\jdk-8u181-windows-x64.exe /s|Out-Null
Write-Host "Java installed"


shutdown /r /t 1