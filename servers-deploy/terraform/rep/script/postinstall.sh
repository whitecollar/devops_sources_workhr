#!/bin/bash

#apt-get -y install vim curl mc rabbitmq-server
apt-get update
#apt-get install 
rabbitmq-plugins enable rabbitmq_management
rabbitmqctl add_user $1 $2
rabbitmqctl set_user_tags $1 administrator
rabbitmqctl set_permissions -p / $1 ".*" ".*" ".*"
echo $1 $2 > /home/lex/rabbit 

wget https://repo.zabbix.com/zabbix/4.0/ubuntu/pool/main/z/zabbix-release/zabbix-release_4.0-2+xenial_all.deb
dpkg -i zabbix-release_4.0-2+xenial_all.deb
apt update
apt-get install -y zabbix-agent
touch /etc/zabbix/zabbix_agentd.conf
echo -e "PidFile=/var/run/zabbix/zabbix_agentd.pid \nLogFile=/var/log/zabbix/zabbix_agentd.log \nLogFileSize=0 \nServer=zabbix.bit-erp.loc \nServerActive=zabbix.bit-erp.loc \nHostMetadataItem=system.uname \nHostMetadata=Infrastructure_linux \n# Hostname=" > /etc/zabbix/zabbix_agentd.conf
systemctl enable zabbix-agent
systemctl restart zabbix-agent.service

mkdir /root/.ssh/
echo -e "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDvNnVB+/go550qVkilJkT5DzzTz1gKA2LI01oqq/C+qSiHlgZ2Q1JuRBO/N0apFs6f9CDzIY9SVDFwWXd+VMbjW44KhQmhgpeFVMP/9vu18/6swpg+4s0bzZjqOO3cLuLjxEQJ+AEjGZSS5pA4tR79lKAl0zP/Aaj/KxxmPgRc0+4RevSxdaTjpx5O8rdb0150yjXYX+OpKVfFh47s7sYbm7pLgdYEM0FsFhEJpZkqj7NyJaCguGiDtqb/y3dSoTjwWfHmk0JK9RHFp+oT9KkzdyRbL4ApljutLPkjqVYmFzCZsIy2Z6sgfPBsxyGhcf1AsMeNNR62E0teM8jmwmfdGF+EWFyx+kFGK1neLw96UR5ZelwfdQgGyHIP7lc4AaI412/ucNVeozjTCKU4KVPiCXXLTfKuLtU4QImKSrem0VkAkYEq42e5gq22TSqhCxHrkLR67rwd4m8lKt1x8cgGVS22ksib+22ZcQnThF8ImXg5iE3G3tUNztxMTsEr/AIZSvq2DBLse4Be4f3hh5fVbPyAzzo3PME+F64VASYviUKdvQDWG7CIe1j2SSTWMs1b3eznKQUxAzA090Lk5rUiLVBWEnRVvH/gyOPsKhSoq0h4uVDFXFJsKT/H5gFJqK9aV2weNq/3dbMHXxDvpK7PZBKAvXSDTL/IDhRpX6qXgQ== root@zabbix \nssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQC1Ii7H+2OQUcnQqdPprJz2PDiXSNqsFhBZWjO2Tp5Ahq+JlsX30iZfhua5sGKJAbPRNSuUALTB8F/QCTbZ/NB6J0D9p7ZUdNnuboGFVF08HMw6lQFfABBkFL2QxI9xOPWT/658HL5xhlai8DlRZBCr5KG5r7gM12rFNLjkxs/5Z//QKdegKVTqiIG1xs8AvhXxtd8GmC7WGWYye8df0Dze1v6Tf2icNMraorrz8t+uZYYIcuSeQNjeTkhUYdU81l1hb9jXwvnZMcGKfAdhijy5tO0RebMy2W44kEuCvqDzhp8TCktrarEU+FEV+3P8SMJ7hXDPXUyX/MLtaF+rynfCAziybb76Aj58kwYvN9UH9QjUqc2NLrGgEbkwy7hHwnCHmoMaIZ3dQN9EYTg+gOX3/HRY+nLUOBnAit9JFK9fIbSn5NIQ4qL8/OUmCrMd8/WXTerXuF2Iq1TgIj4NxU7s3GQXomJCLEbugReAL+JTKEM3Rl6mQh0jMgN0M/DfsbYwoED4Nzb17zWd9YQibPbdZElfLnqq5uNrZJJiZ1p07NeCm7pDGOOkFoWZGaa65V0TvoslOQ0RNYeRd91iJRZJmujpVUNwTuokowbGNC+7I2kVVPH5o8TfYZ43nbdouZYBWJOxqX4hSKzobYdikDULiQVMmv5Kga/o4kbaisLv3w== root@zabbix" >> /root/.ssh/authorized_keys
echo -e "PubkeyAuthentication yes \nAuthorizedKeysFile %h/.ssh/authorized_keys \nRhostsRSAAuthentication no \nHostbasedAuthentication no \nPermitEmptyPasswords no \nPasswordAuthentication no" >> /etc/ssh/sshd_config
chmod 700 /root/.ssh/
chmod 600 /root/.ssh/authorized_keys
service sshd restart
passwd lex -l
useradd admin -p 8M89vwp7vxHPU -G sudo