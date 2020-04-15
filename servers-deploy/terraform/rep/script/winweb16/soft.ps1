# IIS
Install-WindowsFeature -Name Web-Server,Web-Basic-Auth,Web-Windows-Auth,Web-Health,Web-Http-Logging,Web-Request-Monitor,Web-Log-Libraries,Web-Http-Tracing,Web-Common-Http,Web-Default-Doc,Web-Dir-Browsing,Web-Http-Errors,Web-Static-Content,Web-Http-Redirect,Web-Performance,Web-App-Dev,Web-Mgmt-Tools,Web-WebSockets,Web-ISAPI-Ext,Web-ISAPI-Filter,Web-AppInit

# VS Code satup
\\172.16.50.38\share\Distr\VSCodeSetup-x64-1.23.1.exe /verysilent

# VC Redist
\\172.16.50.38\share\Distr\vcredist_x86_13.exe /install /quiet
\\172.16.50.38\share\Distr\vcredist_x64_13.exe /install /quiet

#Git
\\172.16.50.38\share\Distr\Git-2.18.0-64-bit.exe /verysilent

# Java
\\172.16.50.38\share\Distr\jdk-8u181-windows-x64.exe /s

# OneScript
\\172.16.50.38\share\Distr\OneScript-1.0.20-setup.exe /silent

#Far
msiexec /i \\172.16.50.38\share\Distr\Far30b5254.x64.20180805.msi /quiet

#7z
\\172.16.50.38\share\Distr\7z1801-x64.exe /S
