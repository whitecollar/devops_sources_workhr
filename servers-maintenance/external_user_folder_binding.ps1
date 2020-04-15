# Скрипт привязывает конкретного пользователя AD к конкретной группу/папке проекта на внешнем
# обменнике https://share.bit-erp.ru - это нужно для того, чтобы пользователи заказчика могли видеть
# только "свои" папки проектов. Для корректной работы конструкции данный пользователь также должен
# быть включён в группу проекта "для чтения" (<ключ_проекта>_ro) в AD. Запуск:
#
# external_user_folder_binding.ps1 <логин_пользователя_AD> <ключ_проекта> <логин_администратора_Cerberus> <пароль_администратора_Cerberus>
#
# пример:
#
# external_user_folder_binding.ps1 g_71 ADAPTER admin C00lPaSsW0RD
#

param ([string]$UserADLogin, [string]$ProjectKey, [string]$CerberusAdmin, [string]$CerberusAdminPassword)
$GroupName = $ProjectKey.toLower() + "_ro"

java.exe -jar C:\tools\CerberusClient.jar -from $UserADLogin -to $GroupName -srvaddr http://127.0.0.1:10001/service/cerberusftpservice -usr $CerberusAdmin -pwd $CerberusAdminPassword
