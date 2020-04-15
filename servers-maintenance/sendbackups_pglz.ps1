$database1name = "work_base_upp"
$database2name = "ERP_work"
$database3name = "DOC_work"
$backuppath = "h:\backup\full"

$ftpaddr = "ftp://212.116.120.78:2122"
$ftpuser = "webdav|pglzexch"
$ftppass = "*******"
$webclient = New-Object System.Net.WebClient
$webclient.Credentials = New-Object System.Net.NetworkCredential($ftpuser,$ftppass)
$filestosend = Get-ChildItem $backuppath | Where {$_.LastWriteTime -gt (Get-Date).AddHours(-12)} 

foreach ($file in $filestosend | Where-Object {$_.name -like "$database1name*"}) {
    $localfile = $file.FullName
    $webclient.UploadFile($ftpaddr + "/$database1name/" + $file, $localfile)
}
foreach ($file in $filestosend | Where-Object {$_.name -like "$database2name*"}) {
    $localfile = $file.FullName
    $webclient.UploadFile($ftpaddr + "/$database2name/" + $file, $localfile)
}
foreach ($file in $filestosend | Where-Object {$_.name -like "$database3name*"}) {
    $localfile = $file.FullName
    $webclient.UploadFile($ftpaddr + "/$database3name/" + $file, $localfile)
}
