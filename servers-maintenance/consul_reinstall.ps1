$existing = Get-Service | Where-Object {$_.name -like "Consul-client*"}

$consulHome = "C:\consul"
$consulConfig = "C:\consul\etc\consul.d"
$consulService = "Consul-client 1.4"

# remove old Consul-agent if exists

Stop-Service $existing
Start-Sleep -seconds 10
sc.exe delete $existing

# install a new one with commandline arguments

New-Service -Name $consulService -BinaryPathName "$consulHome\consul.exe agent -config-dir=$consulConfig -ui -enable-script-checks $args"

Start-Sleep -Seconds 5
Start-Service -Name $consulService
