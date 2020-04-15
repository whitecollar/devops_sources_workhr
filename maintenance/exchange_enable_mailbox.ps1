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

Enable-Mailbox -Identity $userLogin
Set-Mailbox $userLogin -RetentionPolicy "Delete mails after 2 week"
New-MailContact -Name $userForwardEmail -ExternalEmailAddress $userForwardEmail
Set-Mailbox $userLogin -DeliverToMailboxAndForward $true -ForwardingAddress $userForwardEmail

Remove-PSSession $Session

