

#------------------------------------------------
#Описание сетевой инфраструктуры
#
#------------------------------------------------


resource "vcd_network" "mgt_net" {
    name = "Management Network"
    edge_gateway = "${var.edge_gateway}"
    gateway = "${cidrhost(var.mgt_net_cidr, 1)}"
    static_ip_pool {
        start_address = "${cidrhost(var.mgt_net_cidr, 10)}"
        end_address = "${cidrhost(var.mgt_net_cidr, 200)}"
    }

  provisioner "local-exec" {
    command = "./script/slack/slack.sh ${var.user}  Create_network-Management_Network adreedd_pool_${var.mgt_net_cidr}"
  }
}

resource "vcd_network" "test_net" {
    name = "Test Network"
    edge_gateway = "${var.edge_gateway}"
    gateway = "${cidrhost(var.test_net_cidr, 1)}"
    static_ip_pool {
        start_address = "${cidrhost(var.test_net_cidr, 10)}"
        end_address = "${cidrhost(var.test_net_cidr, 200)}"
    }
      provisioner "local-exec" {
    command = "./script/slack/slack.sh ${var.user}  Create_network-Test_Network adreedd_pool_${var.test_net_cidr}"
  }
}



resource "vcd_network" "int_net_dev" {
    name = "Internal Network Dev"
    edge_gateway = "${var.edge_gateway}"
    gateway = "${cidrhost(var.int_net_dev_cidr, 1)}"
    static_ip_pool {
        start_address = "${cidrhost(var.int_net_dev_cidr, 10)}"
        end_address = "${cidrhost(var.int_net_dev_cidr, 200)}"
    }
        provisioner "local-exec" {
    command = "./script/slack/slack.sh ${var.user}  Create_network-Internal_Network_Dev adreedd_pool_${var.int_net_dev_cidr}"
  } 
}

resource "vcd_network" "int_net_prod" {
    name = "Internal Network Prod"
    edge_gateway = "${var.edge_gateway}"
    gateway = "${cidrhost(var.int_net_prod_cidr, 1)}"
    static_ip_pool {
        start_address = "${cidrhost(var.int_net_prod_cidr, 10)}"
        end_address = "${cidrhost(var.int_net_prod_cidr, 200)}"
    }
           provisioner "local-exec" {
    command = "./script/slack/slack.sh ${var.user}  Create_network-Internal_Network_Prod adreedd_pool_${var.int_net_prod_cidr}"
  }
}   
 
resource "vcd_network" "vpn_net" {
    name = "VPN Internal Network"
    edge_gateway = "${var.edge_gateway}"
    gateway = "${cidrhost(var.vpn_net_cidr, 1)}"
    static_ip_pool {
        start_address = "${cidrhost(var.vpn_net_cidr, 10)}"
        end_address = "${cidrhost(var.vpn_net_cidr, 200)}"
    }
              provisioner "local-exec" {
    command = "./script/slack/slack.sh ${var.user}  Create_network-VPN_Internal_Network adreedd_pool_${var.vpn_net_cidr}"
  }
}
 