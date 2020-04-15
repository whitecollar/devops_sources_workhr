$msipath = "\\172.16.50.38\share\Distr\1C\8.3.12.1529\windows64_8_3_12_1529orca\1CEnterprise 8 Server (x86-64).msi"
$arg = "/qr  TRANSFORMS=1049.mst LANGUAGES=RU"
Start-Process $msipath -ArgumentList $arg