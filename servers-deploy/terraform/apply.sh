#!/bin/bash

REP=../rep
TFBIN=bin/terraform_b
TFSTATELOG="state.log"
SLCHANEL="UAP037F3K"
ENV="dev"


function installDpkg {
        apt install sshpass -y
}

function checkDpkg {
        dpkg -s sshpass
}



function debugOn {
    export TF_LOG=DEBUG
    export TF_TF_LOG_PATH=./debug.log

}

function help {
    echo "-name Имя сервера"
    echo "-os Операционная система nix, win "
    echo "-role Роль сервера"
    echo "-env Окружение"
    echo "-cpu Количество cpu"
    echo "-ram Количество ram"
    echo "-help Справка"
    echo "-listrole Список ролей"
    echo "-listos Список ролей"
    echo "-debugon Режим отладки"

}

function listrole {
            echo "plain - Plain OS, non run postinstall script"
            echo "rabbitmq - RabbitMQ server"
            echo "ci- CI win server (depend -os win)"
            echo "prj- Porject win server (depend -os win)"
            echo "sql- MSSQL win server (depend -os win)"
            echo "work- WORK win server (depend -os win)"
}

function listos {
        echo "win - Windows 2016 Server"
        echo "nix - Linux Ubuntu 16.04"
        }


function runError {
    echo "No parameters found. For help, run ./apply.sh -help "
    RETVAL=100
}

while [ -n "$1" ]
do
case "$1" in
-name) DIR="$2"
echo "Name server $DIR"
shift ;;
-os) OS="$2"
echo "OS $OS"
shift ;;
-role) ROLE="$2"
echo "ROLE $ROLE"
shift ;;
-ou) OU="$2"
echo "OU $OU"
shift ;;
-cpu) CPU="$2"
echo "CPU $OS"
shift ;;
-ram) RAM="$2"
echo "RAM $RAM"
shift ;;
-pass) PASS="$2"
echo "PASS $PASS"
shift ;;
-env) ENV="$2"
echo "Env server $ENV"
shift ;;
-temp) DECLR="$2".tf
echo "Terraform plan $DECLR"
shift ;;
-listrole) listrole
echo "List role";;
-debugon) debugOn
echo "Debug mode on" ;;
-listos) listos
echo "List os";;
-help) help;;
--) shift
break ;;
*) echo "$1 is not an option";;
esac
shift
done
count=1
for param in "$@"
do
echo "Parameter #$count: $param"
count=$(( $count + 1 ))
done



function selectrole {
echo "Select Role"
            if [[ !("$PASS" == null) && ("$ROLE" == rabbitmq) ]]
                then
                APPLYSTRING="-var count=$DIR -var memory=$RAM -var cpus=$CPU -var rabbitAdminPassword=$PASS"
                DECLR="rabbitmq.tf"
                echo Role name RabbitMQ
                else
                if [[ !("$PASS" == null) && ("$ROLE" == sql) ]]
                    then
                    APPLYSTRING="-var count=$DIR -var memory=$RAM -var cpus=$CPU -var saPassword=$PASS -var templateName=wintmpv3"
                    DECLR="sql.tf"
                    echo Role name SQL
                    echo $PASS
                    echo $DECLR
                    echo $APPLYSTRING
                    else
                    if [[("$ROLE" == plain) ]]
                        then
                        APPLYSTRING="-var count=$DIR -var memory=$RAM -var cpus=$CPU -var templateName=wintmpv6"
                        DECLR="plain.tf"
                        echo $DECLR 
                        echo Role name Plain
                        else
                            if [[("$ROLE" == prj) ]]
                                then
                                APPLYSTRING="-var count=$DIR -var memory=$RAM -var cpus=$CPU -var OU=$OU"
                                DECLR="win.tf"
                                echo $DECLR
                                echo Role name Prj
                                else
                                    if [[("$ROLE" == work) ]]
                                        then
                                        APPLYSTRING="-var count=$DIR -var memory=$RAM -var cpus=$CPU -var templateName=wintmpv6 -var OU=$OU"
                                        DECLR="work.tf"
                                        echo $DECLR
                                        echo Role name WORK
                                        else
                                            if [[("$ROLE" == nix) ]]
                                                then
                                                APPLYSTRING="-var count=$DIR -var memory=$RAM -var cpus=$CPU -var templateName=nixv2"
                                                DECLR="nix.tf"
                                                echo $DECLR
                                                echo Role name nixplain
                                                else
                                                 runError   
                                                fi            
                                    fi
                            
                            fi       
                    fi
                fi
            fi  
}

function postinstall {
selfIp="$(cat ./$DIR/selfIp)"
rootAdminLogin="$(cat ./$DIR/rootAdminLogin)"
adminPassword="$(cat ./$DIR/adminassword)"
#postinstallScript="sshpass -p \"$adminPassword\" ssh -o StrictHostKeyChecking=no $rootAdminLogin@$selfIp \"powershell.exe -ExecutionPolicy Bypass -File C:\domain.ps1\""
#postinstallScript="echo end "

if [[ "$OS" == win ]]
        then
        postinstall
        echo os $OS.
        #DECLR="win.tf"
        
        echo $DECLR
       # postinstallScript="sshpass -p \"$adminPassword\" ssh -o StrictHostKeyChecking=no $rootAdminLogin@$selfIp \"powershell.exe -ExecutionPolicy Bypass -File C:\domain.ps1\""
            else
             if [[ "$OS" == nix ]]
                 then
                 postinstall
                echo os $OS.
                #DECLR="nix.tf"
                #echo temp $DECLR
                
else
echo "Parameters OS  incorect.   "
fi
fi
}





function createlink {
mkdir $DIR
cd ./$DIR
ln -s $REP/main.tf ./main.tf
ln -s $REP/$DECLR ./$DECLR
ln -s $REP/$TFBIN ./terraform
ln -s $REP/script ./script
chmod -R +x  ./script/*
chmod +x ./terraform
}

function tfapply {
    selectrole
    createlink
    pwd
    echo \'$ENV\'
    echo \'$SLCHANEL\'
    ./terraform init
    ./terraform plan
     echo $APPLYSTRING
     RETVAL=$?
    ./terraform apply $APPLYSTRING
    #&& $postinstallScript
    ./terraform plan -detailed-exitcode
    RETVAL=$?
    echo RETVAL
    #return ${RETVAL}
    export STATE="OK"


}



if [ -n "$DIR" ]
then
echo TESTEST
echo Name server $DIR
echo Template $DECLR
echo Env $ENV
echo memory $RAM
echo cpu $cpu
tfapply
else
runError
fi
