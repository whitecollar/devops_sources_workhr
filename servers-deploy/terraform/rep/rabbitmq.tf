
#------------------------------------------------
#Переменные по умолчанию
#------------------------------------------------


 

variable "count" {
  default = "nix"
} 
 


variable "templateName" {
  
  default = "packer-ubuntu-16.04-amd64-RabbitMQ"
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
   default = 1024
}

variable "cpus" {
   default = 1
}

 
variable "catalogName" {
   default = "image_high" 
   
}


variable "rabbitAdminLogin" {
   default = "admin"
}

variable "rabbitAdminPassword" {
   default = "s9z8Uj123"
}

variable "rootPassword" {
   default = "Lexroot12qwe"
}
#------------------------------------------------
#Vapp RabbitMQ
#------------------------------------------------


resource "vcd_vapp" "RabbitMQ" {

   name = "${replace(var.count,"'","")}"
   catalog_name  = "${var.catalogName}"
   template_name = "${var.templateName}"
   network_name  =  "${var.env == "dev" ? var.devSubnet : var.prodSubnet}"
   memory        = "${var.memory}"
   cpus          = "${var.cpus}"
   power_on      = true
   initscript    = "echo > /home/lex/complite" 
 

  

 

provisioner "local-exec" {
    when                  = "destroy"
    command = "./script/slack/slack.sh ${var.rabbitAdminLogin} Сервер_RabbitMQ_name:RabbitMQ-${var.count}_удален"
  }

provisioner "file" {
  source      = "./script/postinstall.sh"
  destination = "/home/lex/postinstall.sh"

connection {
    type = "ssh"
    host = "${self.ip}"
    user = "lex"
    password ="${var.rootPassword}"
    
  },

}
/*
provisioner "file" {
  source      = "./script/linux-install.sh"
  destination = "/home/lex/linux-install.sh"

connection {
    type = "ssh"
    host = "${self.ip}"
    user = "lex"
    password ="${var.rootPassword}"
    
  },

}
*/

connection {
    type = "ssh"
    host = "${self.ip}"
    user = "lex"
    password ="${var.rootPassword}"
    }

  provisioner "remote-exec" {

connection {
    type = "ssh"
    user = "lex"
    host = "${self.ip}"
    password ="${var.rootPassword}"
    },


    inline = [
    "echo > /home/lex/testtest",
    "echo ${var.rootPassword} | sudo -S chmod +x /home/lex/postinstall.sh",
    
    "echo ${var.rootPassword} | sudo -S /home/lex/postinstall.sh ${var.rabbitAdminLogin} ${var.rabbitAdminPassword} ${self.ip} ${var.env}",
    ]
  } 


}

#"echo ${var.rootPassword} | sudo -S chmod +x /home/lex/linux-install.sh",
#"echo ${var.rootPassword} | sudo -S /home/lex/linux-install.sh",