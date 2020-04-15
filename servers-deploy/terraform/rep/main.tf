#------------------------------------------------
#Переменные связанные с сетвым окружением
#
#------------------------------------------------

variable "edge_gateway" { default = "ERP_Edge" }
variable "mgt_net_cidr" { default = "172.16.100.0/23" }
variable "int_net_prod_cidr" { default = "172.16.10.0/23" }
variable "int_net_dev_cidr" { default = "172.16.50.0/23" }
variable "test_net_cidr" { default = "172.16.200.0/23" }
variable "vpn_net_cidr" { default = "172.16.250.0/23" }
variable "ext_net" {default="ERP_External"}
variable "ext_ip" { default = "212.116.120.78" }
 variable "ip_rmq" {
  default = "172.16.50.150"
}
variable "slack_user" {default="U560TA5LL"} 
variable "app_nat" {default="NO"} 
variable "sla" {
   default = "\""
}

 


#------------------------------------------------
#Переменные для провайдера услуг
#
#------------------------------------------------

//variable "user" {default ="@admin"}

variable "vcd_org" {default = "ERP"}
variable "vcd_user" {default = "Administrator"}
variable "vcd_pass" {default = "9iJhY71kq"}
variable "vcd_url" {default = "https://vcloud.it-grad.ru/api/" }
variable "vcd_vdc" {default = "ERP_vDC"}
 
#variable "vcd_app" {default = "TERRAFORM"}
variable "vcd_maxRetryTimeout" {default = 2000}

#------------------------------------------------
#Подключение к провайдеру vCloud Director
#
#------------------------------------------------

provider "vcd" {
  user                 = "${var.vcd_user}"
  password             = "${var.vcd_pass}"
  org                  = "${var.vcd_org}"
  url                  = "${var.vcd_url}"
  vdc                  = "${var.vcd_vdc}"
  allow_unverified_ssl = "true"
  maxRetryTimeout      = "${var.vcd_maxRetryTimeout}"
#  allow_unverified_ssl = "${var.vcd_allow_unverified_ssl}"
}



