param(
    [Parameter ()][string]$exchUrl = "",
	[Parameter ()][string]$userLogin = "",
	[Parameter ()][string]$userForwardEmail = "",
	[Parameter ()][string]$exchAdmin = "",
	[Parameter ()][string]$exchPassword = ""
)

$pass = convertto-securestring -AsPlainText -Force -String $exchPassword
$cred = new-object -typename System.Management.Automation.PSCredential -argumentlist $exchAdmin,$pass
$Session = New-PSSession -ConfigurationName Microsoft.Exchange -ConnectionUri $exchUrl -Credential $cred -Authentication Basic -AllowRedirection
Import-PSSession $Session -DisableNameChecking

Remove-MailContact -Identity $userForwardEmail -Confirm:$Y
Remove-Mailbox $userLogin -Confirm:$Y

Remove-PSSession $Session
