
#------------------------------------------------
#Переменные по умолчанию
#------------------------------------------------


variable "app_type" {
  default = "WinPipe"
}

variable "count" {
  default = "dev"
}


variable "template_name_rmq" {
 
  default = "nntemp8"
}

variable "env" {
  default = "dev"
}

variable "prod_subnet" {
 default = "Internal Network Prod"
}

variable "dev_subnet" {
   default = "Internal Network Dev"
}

variable "memory" {
   default = 4096
}

variable "cpus" {
   default = 2
}

 
variable "catalog_name" {
   default = "image_high" 
    
}


variable "user" {
   default = "admin"
   
}

variable "wuser" {
   
    default ="admin" 
}

variable "password" {
   default = "Lexroot12qwe"
}

variable "pw" {
   default = "Lexroot12qwe"
}
#------------------------------------------------
#Vapp WinCI
#------------------------------------------------


resource "vcd_vapp" "win16dev" {

  // name = "${var.count}"
   name = "${replace(var.count,"'","s")}"
   catalog_name  = "${var.catalog_name}"
   template_name = "${var.template_name_rmq}"
   network_name  =  "${var.env == "dev" ? var.dev_subnet : var.prod_subnet}"
  // ip            = "${var.ip_rmq}"
   memory        = "${var.memory}"
   cpus          = "${var.cpus}"
   power_on      = true
   initscript    = "powershell.exe -File c:\\start.ps1"
  
  provisioner "local-exec" {
    command = "echo ${self.guest_ip_addresses.0}  ${self.name} "
}

 
provisioner "local-exec" {
    when                  = "destroy"
    command = "./script/slack/slack.sh ${var.user} Сервер_win_pipeline_name:win_pipe-${var.count}_удален"
  }




provisioner "local-exec" {
   command = "sleep 300"
}



provisioner "file" {
  source      = "./script/windev16/postinstall.ps1"
  destination = "c:\\postinstall.ps1"

connection {
    type = "winrm"
    host = " ${self.guest_ip_addresses.0}"
    user = "admin"
    password ="${var.pw}"
    port = 5986
    https = true
//    use_ntlm = false
    insecure = true
    timeout = "30m"
    //private_key = "${file("~/.ssh/id_rsa")}"
  },

}


provisioner "file" {
  source      = "./script/windev16/domain.ps1"
  destination = "c:\\domain.ps1"

connection {
    type = "winrm"
    host = " ${self.guest_ip_addresses.0}"
    user = "admin"
    password ="${var.pw}"
    port = 5986
    https = true
//    use_ntlm = false
    insecure = true
    timeout = "30m"
    //private_key = "${file("~/.ssh/id_rsa")}"
  },

}


provisioner "file" {
  source      = "./script/windev16/run.ps1"
  destination = "c:\\run.ps1"

connection {
    type = "winrm"
    host = " ${self.guest_ip_addresses.0}"
    user = "admin"
    password ="${var.pw}"
    port = 5986
    https = true
//    use_ntlm = false
    insecure = true
    timeout = "30m"
    //private_key = "${file("~/.ssh/id_rsa")}"
  },

}

connection {
    type = "winrm"
    //user = "admin"
    user = "${var.wuser}"
    host = " ${self.guest_ip_addresses.0}"
    port = 5986
    https = true
//    use_ntlm = false
    insecure = true
    timeout = "30"
    password ="${var.pw}"
    //private_key = "${file("~/.ssh/id_rsa")}"
  },
   

 
provisioner "local-exec" {
//    when                  = "destroy"
    command = "sshpass -p \"${var.pw}\" ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${var.wuser}@${var.ip_rmq} \"powershell.exe -ExecutionPolicy Bypass -File C:\\run.ps1\" "
  }


 


}
