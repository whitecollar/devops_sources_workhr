$consulDistr = "\\share\soft\consul\windows"
$consulHome = "C:\consul\"
$consulConfig = "C:\consul\etc\consul.d"
$dataDir = "C:\consul\var\consul"
$consulService = "Consul-client 1.4"
New-Item -ItemType Directory -Path $consulConfig
New-Item -ItemType Directory -Path $dataDir
Copy-Item "$consulDistr\consul.exe" -Destination $consulHome
Copy-Item "$consulDistr\client-config.json" -Destination $consulConfig

New-Service -Name $consulService -BinaryPathName "$consulHome\consul.exe agent -config-dir=$consulConfig -ui"
Start-Sleep -Seconds 5
Start-Service -Name $consulService
