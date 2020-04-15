$source = "\\share\soft\scripts\jenkins-install"
$jenkinsServiceDirectory = "C:\jenkins\service\connect"
$fsroot = "C:\jenkins\service\data"
$jreVersion = (Get-ChildItem "$env:ProgramFiles\java\" -Exclude "jdk*").Name
$javaPath = "$env:ProgramFiles\java\$jreVersion\bin\java.exe"

if((Test-Path -path $jenkinsServiceDirectory) -ne $true)
{
 New-Item -Path $jenkinsServiceDirectory -ItemType "directory"
 }

Copy-Item "$source\nssm.exe" -Destination $jenkinsServiceDirectory
Copy-Item "$source\swarm-client-3.9.jar" -Destination $jenkinsServiceDirectory
$nssmPath = "$jenkinsServiceDirectory\nssm.exe"
$label = $env:COMPUTERNAME.ToLower()+"service"
$arguments = "-jar $jenkinsServiceDirectory\swarm-client-3.9.jar -master http://172.16.10.14:8080/ -username jenkins -password ""^2kkR92mS)RS"" -executors 3 -labels $label -fsroot $fsroot"
& $nssmPath install jenkins-service $javaPath
& $nssmPath set jenkins-service AppParameters $arguments
& $nssmPath start jenkins-service