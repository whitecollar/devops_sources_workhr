
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
 
  default = "wintmpv6"
}
   
variable "env" {
  default = "dev"
}


variable "OU" {
  default = "Computers" 
//   default = "imagessd" 
    
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
  



provisioner "local-exec" {
    when                  = "destroy"
    command = "./script/slack/slack.sh ${var.user} Сервер_win_pipeline_name:win_pipe-${var.count}_удален"
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
   
  provisioner "local-exec" {
     command = <<EOT
     echo ${self.ip} > ./selfIp
     echo ${var.rootAdminLogin} > ./rootAdminLogin
     echo ${var.adminPassword} > ./adminPassword
     sshpass -p  '${var.adminPassword}' ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${var.rootAdminLogin}@${self.ip} 'powershell.exe -ExecutionPolicy Bypass -File C:\\domain.ps1 ${var.OU}'
     sleep 100
     
     EOT
  }


 
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
     
 

}