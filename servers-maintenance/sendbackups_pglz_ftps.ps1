Add-Type -path "C:\Program Files (x86)\WinSCP\WinSCPnet.dll"

$database1name = "work_base_UPP"
$database2name = "ERP_work"
$database3name = "DOC_work"
$backuppath = "H:\backup\full"

$filestosend = Get-ChildItem $backuppath | Where {$_.LastWriteTime -gt (Get-Date).AddHours(-6)} 

$sessionOptions = New-Object WinSCP.SessionOptions -property @{
    protocol = [WinSCP.Protocol]::FTP
    hostName = "share.bit-erp.ru"
    userName = "pglzexch"
    password = "********"
    FTPSecure = [WinSCP.FtpSecure]::Implicit
}

$session = New-Object WinSCP.Session
$session.Open($sessionOptions)

foreach ($file in $filesToSend | Where-Object {$_.name -like "$database1name*"}) {
    $localFile = $file.FullName
    $session.PutFiles($localfile, "/PGLZ" + "/$database1name/" + $file)
}
foreach ($file in $filesToSend | Where-Object {$_.name -like "$database2name*"}) {
    $localFile = $file.FullName
    $session.PutFiles($localfile, "/PGLZ" + "/$database2name/" + $file)
}
foreach ($file in $filesToSend | Where-Object {$_.name -like "$database3name*"}) {
    $localFile = $file.FullName
    $session.PutFiles($localfile, "/PGLZ" + "/$database3name/" + $file)
}

$session.Dispose()
