
#------------------------------------------------
#Переменные по умолчанию
#------------------------------------------------


variable "app_type" {
  default = "Win"
}

variable "count" {
  default = "dev"
}


variable "templateName" {
 
  default = "wintmpv11"
}
   
variable "env" {
  default = "dev"
}

variable "prodSubnet" {
 default = "Internal Network Prod"
}

variable "devSubnet" {
   default = "Internal Network Dev"
}

variable "memory" {
   default = 4096
}

variable "cpus" {
   default = 2
}

 
variable "catalogName" {
  default = "image_high" 
//   default = "imagessd" 
    
}

variable "OU" {   
  default = "Computers" 
//   default = "imagessd" 
    
}

variable "user" {
   default = "admin"
   
}

variable "rootAdminLogin" {
   
    default ="adm" 
}

variable "postinstallScript" {
   
    default ="postinstallScript" 
}


variable "pw" {
   default = "Lexroot12qwe"
}
variable "adminPassword" {
   default = "s9z8Uj123"
}

#------------------------------------------------
#Vapp Win
#------------------------------------------------


resource "vcd_vapp" "win16" {

  // name = "${var.count}"
   name = "${replace(var.count,"'","")}"
   catalog_name  = "${var.catalogName}"
   template_name = "${var.templateName}"
   network_name  =  "${var.env == "dev" ? var.devSubnet : var.prodSubnet}"
   memory        = "${var.memory}"
   cpus          = "${var.cpus}"
   power_on      = true
   initscript    = "powershell.exe -File c:\\start.ps1"
  /*
     disk {
      name            = "diskV1"
      bus_number = 0
      unit_number = 0
      size            = "64"
      bus_type        = "SCSI"
      bus_sub_type    = "lsilogicsas"
      storage_profile = "IT-GRAD HIGH 12"
        }

    disk {
      name            = "diskV2"
      bus_number = 0
      unit_number = 1
      size            = "240"
      bus_type        = "SCSI"
      bus_sub_type    = "lsilogicsas"
      storage_profile = "IT-GRAD HIGH 12"
         }

    disk {
      name            = "diskV3"
      bus_number = 0
      unit_number = 2
      size            = "50"
      bus_type        = "SCSI"
      bus_sub_type    = "lsilogicsas"
      storage_profile = "IT-GRAD SSD 03"
         }

  
*/



provisioner "local-exec" {
    when                  = "destroy"
    command = "./script/slack/slack.sh ${var.user} Сервер_win_pipeline_name:win_pipe-${var.count}_удален"
  }







provisioner "file" {
  source      = "../../../software-postinstall/project-server.ps1"
  destination = "c:/postinstall.ps1"

connection {
    type = "ssh"
    host = "${self.ip}"
    user = "${var.rootAdminLogin}"
    password ="${var.adminPassword}"
    
  },

}
   

provisioner "file" {
  source      = "./script/windev16/domain.ps1"
  destination = "c:/domain.ps1"

connection {
    type = "ssh"
    host = "${self.ip}"
    user = "${var.rootAdminLogin}"
    password ="${var.adminPassword}"
    
  },

}


provisioner "file" {
  source      = "../../../software-installation/zabbix/install.ps1"
  destination = "c:/zabbix.ps1"

connection {
    type = "ssh"
    host = "${self.ip}"
    user = "${var.rootAdminLogin}"
    password ="${var.adminPassword}"
    
  },

}



  provisioner "local-exec" {
     command = <<EOT
     echo ${self.ip} > ./selfIp
     echo ${var.rootAdminLogin} > ./rootAdminLogin
     echo ${var.adminPassword} > ./adminPassword
     sshpass -p  '${var.adminPassword}' ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${var.rootAdminLogin}@${self.ip} 'powershell.exe -ExecutionPolicy Bypass -File C:\\domain.ps1 ${var.OU}'
     sleep 100
     sshpass -p  '${var.adminPassword}' ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${var.rootAdminLogin}@${self.ip} 'powershell.exe -ExecutionPolicy Bypass -File C:\\postinstall.ps1'
     
    
     EOT
  }


#sshpass -p  '${var.adminPassword}' ssh -o StrictHostKeyChecking=no ${var.rootAdminLogin}@${self.ip} 'powershell.exe -ExecutionPolicy Bypass -File C:\\postinstall.ps1'
#sshpass -p  '${var.adminPassword}' ssh -o StrictHostKeyChecking=no ${var.rootAdminLogin}@${self.ip} 'powershell.exe -ExecutionPolicy Bypass -File C:\\postinstall.ps1'
#command = "./script/slack/slack.sh ${var.user} Сервер_:${var.count}_создан"
/*
provisioner "local-exec" {
    command = "sleep 100"
    }
 */
 provisioner "remote-exec" {
   inline = [
      "powershell.exe -File c:\\domain.ps1",
      "ipconfig /all",
    ]

connection {
    type = "ssh"
    user = "${var.rootAdminLogin}"
    host = "${self.ip}"
    password ="${var.adminPassword}"
    }

    
  } 
     
/*
provisioner "local-exec" {
     command = "sshpass -p \"${var.adminPassword}\" ssh -o StrictHostKeyChecking=no ${var.rootAdminLogin}@${self.ip} \"powershell.exe -ExecutionPolicy Bypass -File C:\\domain.ps1\" "
     command = "sleep 300"
}

provisioner "local-exec" {
    command = "sshpass -p  \"${var.adminPassword}\" ssh -o StrictHostKeyChecking=no ${var.rootAdminLogin}@${self.ip} 'powershell.exe -ExecutionPolicy Bypass -File C:\\domain.ps1'" 
    command = "sshpass -p  \"${var.adminPassword}\" ssh -o StrictHostKeyChecking=no ${var.rootAdminLogin}@${self.ip} 'echo > c:\\1.txt'"
    command = "./script/slack/slack.sh ${var.user} Сервер_win_pipeline_name:win_pipe-${var.count}_создан"
    //command = "./script/slack/slack.sh ${var.user} Сервер_win_name:win-${var.count} ${var.ip_rmq} ${var.user} ${var.password} ${var.app_type} ${var.app_nat}"
    //command = "./script/slack/slack.sh ${var.slack_user} Сервер_win_name:win-${var.count} ${var.ip_rmq} ${var.user} ${var.password} ${var.app_type}   ${substr (var.app_nat, 1, length(var.app_nat)-3)}${var.sla}"
}
*/

}