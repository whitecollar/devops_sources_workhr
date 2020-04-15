Param (
    [Parameter(Mandatory=$true)]
    [string] $v8ver
)
$source = "\\share\soft"

# 1C
#$v8ver = "8.3.12.1685"
$distrpath = "$source\1C\$v8ver\windows64full"
$msiname = "1CEnterprise 8 (x86-64).msi"
$args = " /qn /passive TRANSFORMS=adminstallrelogon.mst;1049.mst DESIGNERALLCLIENTS=1 THICKCLIENT=1 THINCLIENTFILE=1 THINCLIENT=1 SERVERCLIENT=1 LANGUAGES=RU"
$dest = "$env:TEMP\1C"
Copy-Item $distrpath -Destination $dest -Recurse 
Start-Process -Wait $dest\$msiname -ArgumentList $args
Remove-Item $dest -Recurse -Force
